package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface Clock extends Dynamics<Duration, Clock> {
  @Override
  default Clock step(Duration t) {
    final Duration newTime = extract().plus(t);
    return () -> newTime;
  }

  static Clock clock(Duration startingTime) {
    return () -> startingTime;
  }
}
