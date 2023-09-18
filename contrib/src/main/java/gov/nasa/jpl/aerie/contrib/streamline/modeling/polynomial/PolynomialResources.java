package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareOperations;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Arrays;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.shift;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.replaying;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.dynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources.extend;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public final class PolynomialResources {
  private PolynomialResources() {}

  public static Resource<Polynomial> constant(double value) {
    var dynamics = neverExpiring(polynomial(value));
    return () -> dynamics;
  }

  public static UnitAware<Resource<Polynomial>> constant(UnitAware<Double> quantity) {
    return unitAware(constant(quantity.value()), quantity.unit());
  }

  public static Resource<Polynomial> asPolynomial(Resource<Discrete<Double>> discrete) {
    return map(discrete, d -> polynomial(d.extract()));
  }

  public static UnitAware<Resource<Polynomial>> asPolynomial(UnitAware<Resource<Discrete<Double>>> discrete) {
    return unitAware(asPolynomial(discrete.value()), discrete.unit());
  }

  public static Resource<Polynomial> add(Resource<Polynomial> p, Resource<Polynomial> q) {
    return map(p, q, Polynomial::add);
  }

  public static Resource<Polynomial> subtract(Resource<Polynomial> p, Resource<Polynomial> q) {
    return map(p, q, Polynomial::subtract);
  }

  public static Resource<Polynomial> multiply(Resource<Polynomial> p, Resource<Polynomial> q) {
    return map(p, q, Polynomial::multiply);
  }

  public static Resource<Polynomial> divide(Resource<Polynomial> p, Resource<Discrete<Double>> q) {
    return map(p, q, (p$, q$) -> p$.divide(q$.extract()));
  }

  public static Resource<Polynomial> integrate(Resource<Polynomial> p, double startingValue) {
    return integrationCell(p, startingValue);
  }

  private static CellResource<Polynomial> integrationCell(Resource<Polynomial> p, double startingValue) {
    var cell = cellResource(p.getDynamics().data().integral(startingValue));
    // TODO: Use an efficient repeating task here
    whenever(() -> dynamicsChange(p), () -> {
      var p$ = p.getDynamics().data();
      cell.emit(effect(integralDynamics -> p$.integral(integralDynamics.extract())));
    });
    return cell;
  }

  /**
   * Integrates {@param integrand}, but clamps the value.
   * Clamping is done by adjusting the integrand,
   * so that values change immediately when rate reverses.
   * @param integrand
   * @param startingValue
   * @param minimum
   * @param maximum
   * @return
   */
  public static Resource<Polynomial> clampedIntegrate(Resource<Polynomial> integrand, double startingValue, double minimum, double maximum) {
    if (maximum <= minimum) {
      throw new IllegalArgumentException("Maximum (" + maximum + ") is less than/equal to minimum (" + minimum + ")");
    }
    if (startingValue > maximum || startingValue < minimum) {
      throw new IllegalArgumentException("Starting value (" + startingValue + ") out of bounds: [" + minimum + "," + maximum + "]");
    }


    CellResource<Polynomial> resultCopy = CellResource.cellResource(integrand.getDynamics().data().integral(startingValue));
    var lte = lessThanOrEquals(resultCopy, maximum);
    var gte = greaterThanOrEquals(resultCopy, minimum);
    var effectiveIntegrand = map(
      integrand, gte, lte,
      (integrand$, gte$, lte$) -> {
        if (!lte$.extract() && integrand$.lessThanOrEquals(0.0).data().extract()) {
          // we have exceeded the upper limit BUT we have a negative or 0 rate/integrand
          // SHOULD WORK FOR ANY DEGREE INTEGRAND - IF DERIVATIVE IS NEGATIVE IT WILL DECREASE, SO
          //      DERIVATIVE BECOMING POSITIVE ONLY HAS CONSEQUENCE IF WE GO BACK TO HITTING CLAMP LIMIT
          //      WHICH WOULD TRIGGER THIS CHECK AGAIN AS WE HIT GTE.
          return integrand$;
        }
        else if (!gte$.extract() && integrand$.greaterThanOrEquals(0.0).data().extract()) {
          // we have fallen under the lower limit BUT we have a positive or 0 rate/integrand
          return integrand$;
        }
        else if (lte$.extract() && gte$.extract()) {
          return integrand$;
        }
        else {
          return Polynomial.polynomial(0.0);
        }
      }
    );
    var result = integrate(effectiveIntegrand, startingValue);
    // TODO: Use an efficient repeating task here
    spawn(() -> {
      while (true) {
        waitUntil(dynamicsChange(result));
        var resultDynamics = result.getDynamics();
        resultCopy.emit(ignored -> resultDynamics);
      }
    });
    return result;
  }

  public static Resource<Polynomial> clampedIntegrate(Resource<Polynomial> integrand, double startingValue, Resource<Polynomial> minimum, Resource<Polynomial> maximum) {
    return clampedIntegrate2(integrand, startingValue, minimum, maximum).integral;
  }

  public static Resource<Polynomial> clampedIntegrate1(Resource<Polynomial> integrand, double startingValue, Resource<Polynomial> minimum, Resource<Polynomial> maximum) {
    if (startingValue > maximum.getDynamics().data().extract() || startingValue < minimum.getDynamics().data().extract()) {
      throw new IllegalArgumentException("Starting value (" + startingValue + ") out of initial bounds: [" + minimum.getDynamics().data().extract() + "," + maximum.getDynamics().data().extract() + "]");
    }

    CellResource<Polynomial> resultCopy = CellResource.cellResource(integrand.getDynamics().data().integral(startingValue));
    var lte = lessThanOrEquals(resultCopy, maximum);
    var gte = greaterThanOrEquals(resultCopy, minimum);
    var effectiveIntegrand = map(
        integrand, gte, lte,
        (integrand$, gte$, lte$) -> {
          if (!lte$.extract() && integrand$.lessThanOrEquals(0.0).data().extract()) {
            // we have exceeded the upper limit BUT we have a negative or 0 rate/integrand
            // SHOULD WORK FOR ANY DEGREE INTEGRAND - IF DERIVATIVE IS NEGATIVE IT WILL DECREASE, SO
            //      DERIVATIVE BECOMING POSITIVE ONLY HAS CONSEQUENCE IF WE GO BACK TO HITTING CLAMP LIMIT
            //      WHICH WOULD TRIGGER THIS CHECK AGAIN AS WE HIT GTE.
            return integrand$;
          }
          else if (!gte$.extract() && integrand$.greaterThanOrEquals(0.0).data().extract()) {
            // we have fallen under the lower limit BUT we have a positive or 0 rate/integrand
            return integrand$;
          }
          else if (lte$.extract() && gte$.extract()) {
            return integrand$;
          }
          else {
            return Polynomial.polynomial(0.0);
          }
        }
    );
    var result = integrate(effectiveIntegrand, startingValue);
    // TODO: Use an efficient repeating task here
    spawn(() -> {
      while (true) {
        waitUntil(dynamicsChange(result));
        var resultDynamics = result.getDynamics();
        resultCopy.emit(ignored -> resultDynamics);
      }
    });

    // check, if minimum and maximum are changing values or have changed, that min < max still (else err out)
    spawn(() -> {
      while (true) {
        if (lessThanOrEquals(maximum, minimum).getDynamics().data().extract()) {
          throw new IllegalArgumentException("Maximum (" + maximum.getDynamics().data() + " -> " + maximum.getDynamics().data().extract() + ") is less than/equal to minimum (" + minimum.getDynamics().data().extract() + " -> " + minimum.getDynamics().data().extract() + ")");
        }
        waitUntil(dynamicsChange(lessThanOrEquals(maximum, minimum)));
        throw new IllegalArgumentException("Maximum (" + maximum.getDynamics().data() + " -> " + maximum.getDynamics().data().extract() + ") is less than/equal to minimum (" + minimum.getDynamics().data().extract() + " -> " + minimum.getDynamics().data().extract() + ")");
        }
    });
    return result;
  }

  public static ClampedIntegrateReturn clampedIntegrate2(Resource<Polynomial> integrand, double startingValue, Resource<Polynomial> minimum, Resource<Polynomial> maximum) {
    whenever(lessThan(maximum, minimum), () -> {
      throw new IllegalStateException(
          "Inverted bounds for clamped integral: maximum %f < minimum %f"
              .formatted(currentValue(maximum), currentValue(minimum)));
    });
    // Clamp the starting value so integral always starts out legal:
    double clampedStartingValue = Math.min(Math.max(startingValue, currentValue(minimum)), currentValue(maximum));
    // Bootstrap integral by initially using a constant "integral" resource:
    var initialEffectiveIntegrand = clampedEffectiveIntegrand(integrand, minimum, maximum, constant(clampedStartingValue));
    // This way, the cell is initialized to the correct dynamics.
    CellResource<Polynomial> cell = cellResource(initialEffectiveIntegrand.getDynamics().data().integral(clampedStartingValue));
    Resource<Polynomial> effectiveIntegrand = clampedEffectiveIntegrand(integrand, minimum, maximum, cell);
    whenever(() -> dynamicsChange(effectiveIntegrand), () -> {
      var integrandDynamics = effectiveIntegrand.getDynamics().data();
      cell.emit(effect(integralDynamics -> integrandDynamics.integral(integralDynamics.extract())));
    });
    var underflow = integrationCell(max(subtract(effectiveIntegrand, integrand), constant(0)), 0);
    var overflow = integrationCell(max(subtract(integrand, effectiveIntegrand), constant(0)), 0);
    // correction for discretely changing bounds / overshoots due to discretization of time
    whenever(greaterThan(cell, maximum), () -> {
      addToValue(overflow, currentValue(cell) - currentValue(maximum));
      setValue(cell, currentValue(maximum));
    });
    whenever(lessThan(cell, minimum), () -> {
      addToValue(underflow, currentValue(minimum) - currentValue(cell));
      setValue(cell, currentValue(minimum));
    });
    return new ClampedIntegrateReturn(cell, underflow, overflow);
  }

  public record ClampedIntegrateReturn(Resource<Polynomial> integral, Resource<Polynomial> underflow, Resource<Polynomial> overflow) {}

  private static Resource<Polynomial> clampedEffectiveIntegrand(
      Resource<Polynomial> integrand,
      Resource<Polynomial> minimum,
      Resource<Polynomial> maximum,
      Resource<Polynomial> integral)
  {
    var empty = lessThanOrEquals(integral, minimum);
    var full = greaterThanOrEquals(integral, maximum);
    return bind(
        empty, empty$ -> bind(full, full$ -> empty$.extract()
            ? max(integrand, differentiate(minimum))
            : full$.extract()
                ? min(integrand, differentiate(maximum))
                : integrand));
  }

  private static void setValue(CellResource<Polynomial> cell, double value) {
    cell.emit(effect(integralDynamics -> {
      double[] coefficients = Arrays.copyOf(integralDynamics.coefficients(), integralDynamics.coefficients().length);
      coefficients[0] = value;
      return polynomial(coefficients);
    }));
  }

  private static void addToValue(CellResource<Polynomial> cell, double delta) {
    cell.emit(effect(integralDynamics -> {
      double[] coefficients = Arrays.copyOf(integralDynamics.coefficients(), integralDynamics.coefficients().length);
      coefficients[0] += delta;
      return polynomial(coefficients);
    }));
  }

  public static Resource<Polynomial> differentiate(Resource<Polynomial> p) {
    return map(p, Polynomial::derivative);
  }

  public static Resource<Polynomial> movingAverage(Resource<Polynomial> p, Duration interval) {
    var pIntegral = integrate(p, 0);
    var shiftedIntegral = shift(pIntegral, interval, polynomial(0));
    return divide(subtract(pIntegral, shiftedIntegral), DiscreteResourceMonad.unit(interval.ratioOver(SECOND)));
  }

  public static Resource<Discrete<Boolean>> greaterThan(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> () -> p$.greaterThan(threshold));
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> () -> p$.greaterThanOrEquals(threshold));
  }

  public static Resource<Discrete<Boolean>> lessThan(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> () -> p$.lessThan(threshold));
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> () -> p$.lessThanOrEquals(threshold));
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
    return map(p, q, lessThan(p, q),
               (p$, q$, chooseP) -> chooseP.extract() ? p$ : q$);
  }

  public static Resource<Polynomial> max(Resource<Polynomial> p, Resource<Polynomial> q) {
    return map(p, q, greaterThan(p, q),
               (p$, q$, chooseP) -> chooseP.extract() ? p$ : q$);
  }

  public static Resource<Polynomial> clamp(Resource<Polynomial> p, Resource<Polynomial> lowerBound, Resource<Polynomial> upperBound) {
    return max(lowerBound, min(upperBound, p));
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
    return max(lowerBound, min(upperBound, p));
  }
}
