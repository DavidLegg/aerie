package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface Discrete<V> extends Dynamics<V, Discrete<V>> {
  @Override
  V extract();

  @Override
  default Discrete<V> step(Duration t) {
    return this;
  }

  static <V> Discrete<V> discrete(V value) {
    return () -> value;
  }
}
