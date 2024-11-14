package gov.nasa.jpl.aerie.contrib.streamline.modeling.linear;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

// TODO: Implement better support for going to/from Linear
@AutoValueMapper.Record
public record Linear(Double value, Double rate) implements Dynamics<Double, Linear> {
  @Override
  public Double extract() {
    return value;
  }

  @Override
  public Linear step(Duration t) {
    return linear(value + t.ratioOver(SECOND) * rate(), rate());
  }

  public static Linear linear(double value, double rate) {
    return new Linear(value, rate);
  }
}
