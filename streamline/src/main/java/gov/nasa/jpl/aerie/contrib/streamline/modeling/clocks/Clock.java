package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

@AutoValueMapper.Record
public record Clock(Duration time) implements Dynamics<Duration, Clock> {
  @Override
  public Duration extract() {
    return time;
  }

  @Override
  public Clock step(Duration t) {
    return clock(time.plus(t));
  }

  public static Clock clock(Duration startingTime) {
    return new Clock(startingTime);
  }
}
