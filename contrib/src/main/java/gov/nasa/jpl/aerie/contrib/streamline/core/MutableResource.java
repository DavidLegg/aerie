package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Context;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.pure;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.*;
import static java.util.stream.Collectors.joining;

/**
 * A resource to which effects can be applied.
 */
public interface MutableResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(DynamicsEffect<D> effect);
  default void emit(String effectName, DynamicsEffect<D> effect) {
    name(effect, effectName);
    emit(effect);
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(D initial) {
    return resource(pure(initial));
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(D initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    return resource(pure(initial), effectTrait);
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(ErrorCatching<Expiring<D>> initial) {
    // Use autoEffects for a generic CellResource, on the theory that most resources
    // have relatively few effects, and even fewer concurrent effects, so this is performant enough.
    // If that doesn't hold, a more specialized solution can be constructed directly.
    return resource(initial, autoEffects());
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(ErrorCatching<Expiring<D>> initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    MutableResource<D> result = new MutableResource<>() {
      // MD: It was at this moment that Matt realized that MutableResource is an interface. But why...?
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
    if (MutableResourceFlags.DETECT_BUSY_CELLS) {
      result = Profiling.profileEffects(result);
    }
    return result;
  }

  // MD: Musing... I wonder if we want to encourage MutableResource to be used by end-users, or by "library" authors.
  // i.e. does this slot in between `Register` and `Cell`, or does it replace `Register`?
  static <D extends Dynamics<?, D>> void set(MutableResource<D> resource, D newDynamics) {
    resource.emit("Set " + newDynamics, DynamicsMonad.effect(x -> newDynamics));
  }

  static <D extends Dynamics<?, D>> void set(MutableResource<D> resource, Expiring<D> newDynamics) {
    resource.emit("Set " + newDynamics, ErrorCatchingMonad.<Expiring<D>, Expiring<D>>map($ -> newDynamics)::apply);
  }

  /**
   * Turn on busy cell detection.
   *
   * <p>
   *     Calling this method once before constructing your model will profile effects on every cell.
   *     Profiling effects may be compute and/or memory intensive, and should not be used in production.
   * </p>
   * <p>
   *     If only a few cells are suspect, you can also call {@link Profiling#profileEffects}
   *     directly on just those cells, rather than profiling every cell.
   * </p>
   * <p>
   *     Call {@link Profiling#dump()} to see results.
   * </p>
   */
  static void detectBusyCells() {
    MutableResourceFlags.DETECT_BUSY_CELLS = true;
  }
}

/**
 * Private global flags for configuring cell resources for debugging.
 * Flags here are meant to be set once before constructing the model,
 * and to apply to every cell that gets built.
 */
final class MutableResourceFlags {
  public static boolean DETECT_BUSY_CELLS = false;
}

/*
Add MutableResource

Adds MutableResource and supporting types for defining cells and effects.

This design emphasizes separation of concerns in two primary ways:
* First, since every resource dynamics carries an expiry and stepping behavior,
  we don't need to define a different cell type depending on how that value is computed (like an Accumulator)
  nor by what kind of dynamics are stored (e.g. Discrete vs. Real).
* Second, since the DynamicsEffect interface defines a fully general effect type,
  we don't need to define a different cell type depending on the supported class of effects.
  Taken together, we can define a single cell type.

This design also seeks to reduce overhead for modelers to handle effects the "right" way.
By this, we mean using semantically correct effects, rather than (ab)using Registers for everything.
* Instead of defining a new type for effects, we use a general DynamicsEffect interface.
  We also have the DynamicsMonad.effect method, so effects can be written against the base dynamics type,
  often as a small in-line lambda.
* To support these "black-box" effects, we use an "automatic" effect trait by default,
  which tests concurrent effects for commutativity.
  Since effects are rarely concurrent in practice, this is performant enough in most use cases.
  Furthermore, it combines with the error-handling wrapper to bubble-up useful error messages,
  as well as let independent portions of the simulation continue normally.

Taken together, the above means there's a single "default" way to build a cell,
which provides enough flexibility and performance for most use cases.
 */
