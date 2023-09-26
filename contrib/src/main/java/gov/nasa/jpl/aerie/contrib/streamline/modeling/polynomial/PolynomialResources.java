package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareOperations;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Arrays;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.success;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.shift;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.bindEffect;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.unit;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources.extend;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public final class PolynomialResources {
  private PolynomialResources() {}

  public static Resource<Polynomial> constant(double value) {
    var dynamics = unit(polynomial(value));
    return () -> dynamics;
  }

  public static UnitAware<Resource<Polynomial>> constant(UnitAware<Double> quantity) {
    return unitAware(constant(quantity.value()), quantity.unit());
  }

  public static Resource<Polynomial> asPolynomial(Resource<Discrete<Double>> discrete) {
    return ResourceMonad.map(discrete, d -> polynomial(d.extract()));
  }

  public static UnitAware<Resource<Polynomial>> asPolynomial(UnitAware<Resource<Discrete<Double>>> discrete) {
    return unitAware(asPolynomial(discrete.value()), discrete.unit());
  }

  public static Resource<Polynomial> add(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.map(p, q, Polynomial::add);
  }

  public static Resource<Polynomial> subtract(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.map(p, q, Polynomial::subtract);
  }

  public static Resource<Polynomial> multiply(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.map(p, q, Polynomial::multiply);
  }

  public static Resource<Polynomial> divide(Resource<Polynomial> p, Resource<Discrete<Double>> q) {
    return ResourceMonad.map(p, q, (p$, q$) -> p$.divide(q$.extract()));
  }

  public static Resource<Polynomial> integrate(Resource<Polynomial> integrand, double startingValue) {
    var cell = cellResource(map(integrand.getDynamics(), (Polynomial $) -> $.integral(startingValue)));
    // Use integrand's expiry but not integral's, since we're refreshing the integral
    wheneverDynamicsChange(integrand, integrandDynamics ->
        cell.emit(bindEffect(integral -> DynamicsMonad.map(integrandDynamics, integrand$ ->
            integrand$.integral(integral.extract())))));
    return cell;
  }

  public static Resource<Polynomial> clampedIntegrate(Resource<Polynomial> integrand, double startingValue, Resource<Polynomial> minimum, Resource<Polynomial> maximum) {
    // Clamp the starting value so integral always starts out legal:
    var clampedStartingValue = clamp(constant(startingValue), minimum, maximum);
    // Bootstrap integral by initially using a constant "integral" resource:
    var initialEffectiveIntegrand = clampedEffectiveIntegrand(integrand, minimum, maximum, clampedStartingValue);
    // This way, the cell is initialized to the correct dynamics.
    var cell = cellResource(initialEffectiveIntegrand.getDynamics().map($ -> neverExpiring($.data().integral(currentValue(clampedStartingValue)))));
    var effectiveIntegrand = clampedEffectiveIntegrand(integrand, minimum, maximum, cell);
    // Use integrand's expiry but not integral's, since we're refreshing the integral
    wheneverDynamicsChange(effectiveIntegrand, integrandDynamics -> {
      cell.emit(bindEffect(integral -> ErrorCatchingMonad.map(integrandDynamics, integrand$ ->
          neverExpiring(integrand$.data().integral(integral.extract())))));
    });
    // correction for discretely changing bounds / overshoots due to discretization of time
    whenever(greaterThan(cell, maximum), () -> setValue(cell, currentValue(maximum)));
    whenever(lessThan(cell, minimum), () -> setValue(cell, currentValue(minimum)));
    return cell;
  }

  private static Resource<Polynomial> clampedEffectiveIntegrand(
      Resource<Polynomial> integrand,
      Resource<Polynomial> minimum,
      Resource<Polynomial> maximum,
      Resource<Polynomial> integral)
  {
    var impossible = lessThan(maximum, minimum);
    var empty = lessThanOrEquals(integral, minimum);
    var full = greaterThanOrEquals(integral, maximum);
    return bind(impossible, empty, full, (impossible$, empty$, full$) -> {
      if (impossible$.extract()) {
        throw new IllegalStateException(
            "Inverted bounds for clamped integral: maximum %f < minimum %f"
                .formatted(currentValue(maximum), currentValue(minimum)));
      }
      Resource<Polynomial> result = integrand;
      if (empty$.extract()) {
        result = max(result, differentiate(minimum));
      }
      if (full$.extract()) {
        result = min(result, differentiate(maximum));
      }
      return result;
    });
  }

  private static void setValue(CellResource<Polynomial> cell, double value) {
    cell.emit(effect(integralDynamics -> {
      double[] coefficients = Arrays.copyOf(integralDynamics.coefficients(), integralDynamics.coefficients().length);
      coefficients[0] = value;
      return polynomial(coefficients);
    }));
  }

  public static Resource<Polynomial> differentiate(Resource<Polynomial> p) {
    return ResourceMonad.map(p, Polynomial::derivative);
  }

  public static Resource<Polynomial> movingAverage(Resource<Polynomial> p, Duration interval) {
    var pIntegral = integrate(p, 0);
    var shiftedIntegral = shift(pIntegral, interval, polynomial(0));
    return divide(subtract(pIntegral, shiftedIntegral), DiscreteResourceMonad.unit(interval.ratioOver(SECOND)));
  }

  public static Resource<Discrete<Boolean>> greaterThan(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.greaterThan(threshold)));
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.greaterThanOrEquals(threshold)));
  }

  public static Resource<Discrete<Boolean>> lessThan(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.lessThan(threshold)));
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.lessThanOrEquals(threshold)));
  }

  public static Resource<Discrete<Boolean>> greaterThan(Resource<Polynomial> p, Resource<Polynomial> q) {
    return greaterThan(subtract(p, q), 0);
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Polynomial> p, Resource<Polynomial> q) {
    return greaterThanOrEquals(subtract(p, q), 0);
  }

  public static Resource<Discrete<Boolean>> lessThan(Resource<Polynomial> p, Resource<Polynomial> q) {
    return lessThan(subtract(p, q), 0);
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Polynomial> p, Resource<Polynomial> q) {
    return lessThanOrEquals(subtract(p, q), 0);
  }

  public static Resource<Polynomial> min(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.bind(p, q, (p$, q$) -> ExpiringToResourceMonad.unit(p$.min(q$)));
  }

  public static Resource<Polynomial> max(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.bind(p, q, (p$, q$) -> ExpiringToResourceMonad.unit(p$.max(q$)));
  }

  public static Resource<Polynomial> clamp(Resource<Polynomial> p, Resource<Polynomial> lowerBound, Resource<Polynomial> upperBound) {
    return ResourceMonad.bind(
        lessThan(upperBound, lowerBound),
        impossible -> {
          if (impossible.extract()) {
            throw new IllegalStateException(
                "Inverted bounds for clamp: maximum %f < minimum %f"
                    .formatted(currentValue(upperBound), currentValue(lowerBound)));
          }
          return max(lowerBound, min(upperBound, p));
        });
  }

  private static Polynomial scalePolynomial(Polynomial p, double s) {
    return p.multiply(polynomial(s));
  }

  public static UnitAware<Resource<Polynomial>> unitAware(Resource<Polynomial> p, Unit unit) {
    return UnitAwareResources.unitAware(p, unit, PolynomialResources::scalePolynomial);
  }

  public static UnitAware<CellResource<Polynomial>> unitAware(CellResource<Polynomial> p, Unit unit) {
    return UnitAwareResources.unitAware(p, unit, PolynomialResources::scalePolynomial);
  }

  public static UnitAware<Resource<Polynomial>> add(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return UnitAwareOperations.add(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::add);
  }

  public static UnitAware<Resource<Polynomial>> subtract(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return UnitAwareOperations.subtract(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::subtract);
  }

  public static UnitAware<Resource<Polynomial>> multiply(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return UnitAwareOperations.multiply(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::multiply);
  }

  public static UnitAware<Resource<Polynomial>> divide(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Discrete<Double>>> q) {
    return UnitAwareOperations.divide(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::divide);
  }

  public static UnitAware<Resource<Polynomial>> integrate(UnitAware<Resource<Polynomial>> p, UnitAware<Double> startingValue) {
    return UnitAwareOperations.integrate(extend(PolynomialResources::scalePolynomial), p, startingValue, PolynomialResources::integrate);
  }

  public static UnitAware<Resource<Polynomial>> differentiate(UnitAware<Resource<Polynomial>> p) {
    return UnitAwareOperations.differentiate(extend(PolynomialResources::scalePolynomial), p, PolynomialResources::differentiate);
  }

  // Ugly $ suffix is to avoid ambiguous overloading after erasure.
  public static Resource<Discrete<Boolean>> greaterThan$(UnitAware<Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return greaterThan(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals$(UnitAware<Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return greaterThanOrEquals(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> lessThan$(UnitAware<Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return lessThan(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals$(UnitAware<Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return lessThanOrEquals(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> greaterThan(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return greaterThan(subtract(p, q).value(), 0);
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return greaterThanOrEquals(subtract(p, q).value(), 0);
  }

  public static Resource<Discrete<Boolean>> lessThan(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return lessThan(subtract(p, q).value(), 0);
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return lessThanOrEquals(subtract(p, q).value(), 0);
  }

  public static UnitAware<Resource<Polynomial>> min(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return unitAware(min(p.value(), q.value(p.unit())), p.unit());
  }

  public static UnitAware<Resource<Polynomial>> max(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> q) {
    return unitAware(max(p.value(), q.value(p.unit())), p.unit());
  }

  public static UnitAware<Resource<Polynomial>> clamp(UnitAware<Resource<Polynomial>> p, UnitAware<Resource<Polynomial>> lowerBound, UnitAware<Resource<Polynomial>> upperBound) {
    return unitAware(clamp(p.value(), lowerBound.value(p.unit()), upperBound.value(p.unit())), p.unit());
  }
}
