package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.facades;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Labelled;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

// Facade Style #1 - extends CellResource
// This allows the general emit to be used directly on the facade still, for unusual effects.
// It also means the full library of static effect methods can be used on this state.
public interface SettableState1<T> extends CellResource<Discrete<T>> {
  void set(T value);

  default T get() {
    return currentValue(this);
  }

  // "facade" constructor - wraps a CellResource in a facade
  static <T> SettableState1<T> settableState(CellResource<Discrete<T>> implementation) {
    return new SettableState1<>() {
      @Override
      public void set(final T value) {
        DiscreteEffects.set(this, value);
      }

      @Override
      public void emit(final Labelled<DynamicsEffect<Discrete<T>>> effect) {
        implementation.emit(effect);
      }

      @Override
      public ErrorCatching<Expiring<Discrete<T>>> getDynamics() {
        return implementation.getDynamics();
      }
    };
  }

  // "model" constructor - builds the CellResource as well.
  static <T> SettableState1<T> settableState(T initialValue) {
    return settableState(cellResource(discrete(initialValue)));
  }
}
