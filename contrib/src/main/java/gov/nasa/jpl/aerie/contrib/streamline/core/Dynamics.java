package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface Dynamics<V, D extends Dynamics<V, D>> {
    V extract();

    D step(Duration t);

    default boolean areEqualResults(D left, D right) {
      return left.equals(right);
    }
}
