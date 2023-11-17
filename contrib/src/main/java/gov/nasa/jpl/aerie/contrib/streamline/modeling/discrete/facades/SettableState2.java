package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.facades;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

// Facade Style #1 - extends Resource, but not CellResource
// This fully locks down the effects you can use, the same as the prior resource framework.
public interface SettableState2<T> extends Resource<Discrete<T>> {
  void set(T value);

  default T get() {
    return currentValue(this);
  }

  // "facade" constructor - wraps a CellResource in a facade
  static <T> SettableState2<T> settableState(CellResource<Discrete<T>> implementation) {
    return new SettableState2<>() {
      @Override
      public void set(final T value) {
        DiscreteEffects.set(implementation, value);
      }

      @Override
      public ErrorCatching<Expiring<Discrete<T>>> getDynamics() {
        return implementation.getDynamics();
      }
    };
  }

  // "model" constructor - builds the CellResource as well.
  static <T> SettableState2<T> settableState(T initialValue) {
    return settableState(cellResource(discrete(initialValue)));
  }
}
