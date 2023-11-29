package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Context;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.pure;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.*;
import static java.util.stream.Collectors.joining;

/**
 * Resource which is backed directly by a cell.
 * Effects can be applied to this resource.
 * Effect names are augmented with this resource's name(s).
 */
public interface CellResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(DynamicsEffect<D> effect);
  default void emit(String effectName, DynamicsEffect<D> effect) {
    name(effect, effectName);
    emit(effect);
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial) {
    return cellResource(pure(initial));
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    return cellResource(pure(initial), effectTrait);
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(ErrorCatching<Expiring<D>> initial) {
    return cellResource(initial, autoEffects());
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(ErrorCatching<Expiring<D>> initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    return new CellResource<>() {
      // Use autoEffects for a generic CellResource, on the theory that most resources
      // have relatively few effects, and even fewer concurrent effects, so this is performant enough.
      // If that doesn't hold, a more specialized solution can be constructed directly.
      private final CellRef<DynamicsEffect<D>, Cell<D>> cell = allocate(initial, effectTrait);

      @Override
      public void emit(final DynamicsEffect<D> effect) {
        augmentEffectName(effect);
        cell.emit(effect);
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return cell.get().dynamics;
      }

      private void augmentEffectName(DynamicsEffect<D> effect) {
        String effectName = getName(effect).orElse("anonymous effect");
        String resourceName = getName(this).orElse("anonymous resource");
        String augmentedName = effectName + " on " + resourceName + Context.get().stream().map(c -> " during " + c).collect(joining());
        name(effect, augmentedName);
      }
    };
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, D newDynamics) {
    resource.emit("Set " + newDynamics, DynamicsMonad.effect(x -> newDynamics));
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, Expiring<D> newDynamics) {
    resource.emit("Set " + newDynamics, ErrorCatchingMonad.<Expiring<D>, Expiring<D>>map($ -> newDynamics)::apply);
  }

}
