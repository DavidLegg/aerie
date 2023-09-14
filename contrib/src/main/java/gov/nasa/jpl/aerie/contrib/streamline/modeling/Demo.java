package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.logs.Log;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.logs.LogEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.signalling;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.toggle;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.using;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects.consume;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.asPolynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clamp;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.greaterThan;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.integrate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.lessThan;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.lessThan$;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.unitAware;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Quantities.quantity;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.StandardUnits.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareOperations.simplify;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.commutingEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;

public final class Demo {

  // Unit-naive version of a model, to demonstrate some core concepts:

  // TODO: Actually use the error log
  // Explicitly set commuting effects for the error log,
  // because we don't care about message ordering for concurrent effects.
  // That is, even though the logs aren't "equals", they're equal enough for us.
  // We don't want logging a soft error to throw an exception!
  CellResource<Discrete<Log>> errorLog = cellResource(discrete(Log.empty()), commutingEffects());
  // Consumable, continuous:
  CellResource<Polynomial> fuel_kg = cellResource(polynomial(20.0));
  // Non-consumable, discrete:
  CellResource<Discrete<Double>> power_w = cellResource(discrete(120.0));
  // Atomic non-consumable:
  CellResource<Discrete<Integer>> rwaControl = cellResource(discrete(1));
  // Settable / enum state:
  CellResource<Discrete<OnOff>> enumSwitch = cellResource(discrete(OnOff.ON));
  // Toggle / flag:
  CellResource<Discrete<Boolean>> boolSwitch = cellResource(discrete(true));

  // Derived states:
  Resource<Discrete<OnOff>> derivedEnumSwitch = map(boolSwitch, b -> b ? OnOff.ON : OnOff.OFF);
  Resource<Discrete<Boolean>> errorsAreLogged = map(errorLog, log -> log.size() > 0);
  Resource<Polynomial> batterySOC_J = integrate(asPolynomial(power_w), 100);
  Resource<Discrete<Double>> clampedPower_w = map(power_w, p -> p < 0 ? 0 : p);
  Resource<Polynomial> clampedBatterySOC_J = clamp(batterySOC_J, constant(0), constant(100));
  Resource<Discrete<Boolean>> lowPower = lessThan(batterySOC_J, 20);
  Resource<Discrete<Boolean>> badness = map(
      lowPower, errorsAreLogged, enumSwitch,
      (lowPower$, errors$, switch$) ->
          lowPower$ && errors$ && switch$ == OnOff.OFF);

  {
    spawn(() -> {
      waitUntil(when(badness));
      LogEffects.log(errorLog, "Badness has happened!");
    });

    using(power_w, 10, () -> {
      using(rwaControl, () -> {
        // Consume 5.4 kg of fuel over the next minute, linearly
        consume(fuel_kg, 5.4, Duration.MINUTE);
        // Separately, we could be doing things during that minute.
        delay(Duration.MINUTE);
      });
      set(enumSwitch, OnOff.OFF);
      toggle(boolSwitch);
    });

    set(boolSwitch, false);
  }


  // The exact same model again, but this time made unit-aware throughout.
  // States without units have been re-used instead of being re-defined

  // Consumable, continuous:
  // CellResource<Polynomial> fuel_kg = cellResource(polynomial(20.0));
  UnitAware<CellResource<Polynomial>> fuel = unitAware(
      cellResource(polynomial(20.0)), KILOGRAM);
  // Non-consumable, discrete:
  UnitAware<CellResource<Discrete<Double>>> power = DiscreteResources.unitAware(
      cellResource(discrete(120.0)), WATT);

  UnitAware<Resource<Polynomial>> batterySOC = integrate(asPolynomial(simplify(power)), quantity(100, JOULE));
  UnitAware<Resource<Discrete<Double>>> clampedPower = DiscreteResources.unitAware(map(power.value(WATT), p -> p < 0 ? 0 : p), WATT);
  UnitAware<Resource<Discrete<Double>>> clampedPower_v2 = /* map(power, p -> lessThan(p, quantity(0, WATT)) ? quantity(0, WATT) : p) */
      null;
  UnitAware<Resource<Polynomial>> clampedBatterySOC = clamp(batterySOC, constant(quantity(0, JOULE)), constant(quantity(100, JOULE)));
  Resource<Discrete<Boolean>> lowPower$ = lessThan$(batterySOC, quantity(20, JOULE));

  {
    using(power, quantity(10, WATT), () -> {
      using(rwaControl, () -> {
        // Consume 5.4 kg of fuel over the next minute, linearly
        consume(fuel, quantity(5.4, KILOGRAM), Duration.MINUTE);
        // Separately, we could be doing things during that minute.
        delay(Duration.MINUTE);
      });
      set(enumSwitch, OnOff.OFF);
      toggle(boolSwitch);
    });
  }

  public enum OnOff { ON, OFF }
}
