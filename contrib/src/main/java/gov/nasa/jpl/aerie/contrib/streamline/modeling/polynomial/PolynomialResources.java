package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareOperations;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.shift;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.signalling;
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
    var cell = allocate(p.getDynamics().data().integral(startingValue));
    // TODO: Use an efficient repeating task here
    spawn(() -> {
      while (true) {
        waitUntil(dynamicsChange(p));
        var p$ = p.getDynamics().data();
        System.out.println(p$.toString() + " --> " + currentTime());
        cell.emit(effect(integralDynamics -> p$.integral(integralDynamics.extract())));
      }
    });
    return () -> cell.get().dynamics;
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
