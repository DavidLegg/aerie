package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Labelled.labelled;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.unit;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;

/**
 * Resource which is backed directly by a cell.
 * Effects can be applied to this resource.
 * By default, effects are labelled with the ambient context
 * at the time the effect is emitted.
 *
 * <p>
 *   A typical use case would be to emit an effect from an activity, e.g.
 *   <pre>
 * class ActivityA {
 *   void run() {
 *     {@link Labelled#inContext}("ActivityA", () -> {
 *       {@link CellResource#set}(someResource, newDynamics);
 *       anotherResource.emit("Take the square root", {@link DiscreteDynamicsMonad#effect}(n -> sqrt(n)));
 *     });
 *   }
 * }
 *   </pre>
 * </p>
 */
public interface CellResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(Labelled<DynamicsEffect<D>> effect);
  default void emit(String effectName, DynamicsEffect<D> effect) {
    emit(labelled(effectName, effect));
  }
  default void emit(DynamicsEffect<D> effect) {
    emit("anonymous effect", effect);
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial) {
    return cellResource(unit(initial));
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial, EffectTrait<Labelled<DynamicsEffect<D>>> effectTrait) {
    return cellResource(unit(initial), effectTrait);
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(ErrorCatching<Expiring<D>> initial) {
    return cellResource(initial, autoEffects());
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(ErrorCatching<Expiring<D>> initial, EffectTrait<Labelled<DynamicsEffect<D>>> effectTrait) {
    return new CellResource<>() {
      // Use autoEffects for a generic CellResource, on the theory that most resources
      // have relatively few effects, and even fewer concurrent effects, so this is performant enough.
      // If that doesn't hold, a more specialized solution can be constructed directly.
      private final CellRef<Labelled<DynamicsEffect<D>>, Cell<D>> cell = allocate(initial, effectTrait);

      @Override
      public void emit(final Labelled<DynamicsEffect<D>> effect) {
        cell.emit(effect);
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return cell.get().dynamics;
      }
    };
  }

  static <D extends Dynamics<?, D>> CellResource<D> staticallyCreated(Supplier<CellResource<D>> constructor) {
    return new CellResource<>() {
      private CellResource<D> delegate = constructor.get();

      @Override
      public void emit(final Labelled<DynamicsEffect<D>> effect) {
        actOnCell(() -> delegate.emit(effect));
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        // Keep the field access using () -> ... form, don't simplify to delegate::getDynamics
        // Simplifying will access delegate before calling actOnCell, failing if we need to re-allocate delegate.
        return actOnCell(() -> delegate.getDynamics());
      }

      private void actOnCell(Runnable action) {
        actOnCell(() -> {
          action.run();
          return UNIT;
        });
      }

      private <R> R actOnCell(Supplier<R> action) {
        try {
          return action.get();
        } catch (Scoped.EmptyDynamicCellException | IllegalArgumentException e) {
          // If we're running unit tests, several simulations can happen without reloading the Resources class.
          // In that case, we'll have discarded the clock resource we were using, and get the above exception.
          // REVIEW: Is there a cleaner way to make sure this resource gets (re-)initialized?
          delegate = constructor.get();
          return action.get();
        }
      }
    };
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, D newDynamics) {
    resource.emit("Set " + newDynamics, DynamicsMonad.effect(x -> newDynamics));
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, Expiring<D> newDynamics) {
    resource.emit("Set " + newDynamics, ErrorCatchingMonad.<Expiring<D>, Expiring<D>>lift($ -> newDynamics)::apply);
  }
}
