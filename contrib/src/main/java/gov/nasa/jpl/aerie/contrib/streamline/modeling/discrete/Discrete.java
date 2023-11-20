package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.utils.DoubleUtils;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Objects;
import java.util.function.BiPredicate;

public interface Discrete<V> extends Dynamics<V, Discrete<V>> {
  /**
   * Special constructor for discrete double dynamics.
   *
   * <p>
   *   Uses {@link DoubleUtils#areEqualResults} to compare values from effects.
   * </p>
   */
  static Discrete<Double> discrete(Double value) {
    return discrete(value, DoubleUtils::areEqualResults);
  }

  /**
   * Standard constructor for discrete dynamics.
   *
   * <p>
   *   Compares values using their inherent {@link Object#equals} method.
   *   Use {@link Discrete#discrete(Object, TriFunction)} to override this.
   * </p>
   */
  static <V> Discrete<V> discrete(V value) {
    return discrete(value, (original, x, y) -> Objects.deepEquals(x, y));
  }

  /**
   * General constructor for discrete dynamics.
   *
   * <p>
   *   valueEquality should reflect a (possibly fuzzy) equality check,
   *   used to determine when concurrent effects commute.
   * </p>
   */
  static <V> Discrete<V> discrete(V value, TriFunction<V, V, V, Boolean> valueEquality) {
    return new Discrete<>() {
      @Override
      public V extract() {
        return value;
      }

      @Override
      public Discrete<V> step(final Duration t) {
        return this;
      }

      @Override
      public boolean areEqualResults(final Discrete<V> left, final Discrete<V> right) {
        return valueEquality.apply(value, left.extract(), right.extract());
      }
    };
  }
}
