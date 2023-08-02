package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.signalling;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class ClockResources {
  private ClockResources() {}

  public static Resource<Clock> clock() {
    var cell = allocate(Clock.clock(ZERO));
    return () -> cell.get().dynamics;
  }

  public static Resource<Discrete<Boolean>> lessThan(Resource<Clock> clock, Duration threshold) {
    return signalling(bind(clock, (Clock c) -> () -> {
      final Duration crossoverTime = threshold.minus(c.extract());
      return crossoverTime.isPositive()
          ? expiring(discrete(true), crossoverTime)
          : neverExpiring(discrete(false));
    }));
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Clock> clock, Duration threshold) {
    // Since Duration is an integral type, implement strictness through EPSILON stepping
    return lessThan(clock, threshold.plus(EPSILON));
  }

  public static Resource<Discrete<Boolean>> greaterThan(Resource<Clock> clock, Duration threshold) {
    return DiscreteResourceMonad.map(lessThanOrEquals(clock, threshold), $ -> !$);
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Clock> clock, Duration threshold) {
    return DiscreteResourceMonad.map(lessThan(clock, threshold), $ -> !$);
  }
}
