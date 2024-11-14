package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

@AutoValueMapper.Record
public record Discrete<V>(V value) implements Dynamics<V, Discrete<V>> {
  @Override
  public V extract() {
    return value;
  }

  @Override
  public Discrete<V> step(Duration t) {
    return this;
  }

  public static <V> Discrete<V> discrete(V value) {
    return new Discrete<>(value);
  }
}
