package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;

public interface CellResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(DynamicsEffect<D> effect);

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial) {
    return cellResource(initial, autoEffects());
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    return new CellResource<>() {
      // Use autoEffects for a generic CellResource, on the theory that most resources
      // have relatively few effects, and even fewer concurrent effects, so this is performant enough.
      // If that doesn't hold, a more specialized solution can be constructed directly.
      private final CellRef<DynamicsEffect<D>, Cell<D>> cell = allocate(initial, effectTrait);

      @Override
      public void emit(final DynamicsEffect<D> effect) {
        cell.emit(effect);
      }

      @Override
      public Expiring<D> getDynamics() {
        return cell.get().dynamics;
      }
    };
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, D newDynamics) {
    resource.emit(ExpiringMonad.effect(x -> newDynamics));
  }
}
