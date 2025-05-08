package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Context;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.duration;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.expiry;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.pure;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.*;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling.profile;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling.profileEffects;
import static java.util.stream.Collectors.joining;

/**
 * A resource to which effects can be applied.
 */
public interface MutableResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(DynamicsEffect<D> effect);
  default void emit(String effectName, DynamicsEffect<D> effect) {
    emit(name(effect, effectName));
  }

  static <D extends Dynamics<?, D>> MutableResourceBuilder<D> resource() {
    return new MutableResourceBuilder<>();
  }

  // Add default value overloads, because these specify the type parameter for us, which is often cleaner.
  static <D extends Dynamics<?, D>> MutableResourceBuilder<D> resource(final D defaultValue) {
    return MutableResource.<D>resource().defaultValue(defaultValue);
  }

  static <D extends Dynamics<?, D>> MutableResourceBuilder<D> resource(final ErrorCatching<Expiring<D>> defaultValue) {
    return MutableResource.<D>resource().defaultValue(defaultValue);
  }

  class MutableResourceBuilder<D extends Dynamics<?, D>> extends BaseMutableResourceBuilder<D, MutableResourceBuilder<D>> {}

  class BaseMutableResourceBuilder<D extends Dynamics<?, D>, Self extends BaseMutableResourceBuilder<D, Self>> {
    private String name;
    private ErrorCatching<Expiring<D>> defaultValue;
    private ValueMapper<ErrorCatching<Expiring<D>>> dynamicsMapper;
    private InconBehavior<ErrorCatching<Expiring<D>>> inconBehavior;
    private EffectTrait<DynamicsEffect<D>> effectTrait = autoEffects();

    public Self name(final String name) {
      this.name = name;
      return (Self) this;
    }

    public Self defaultValue(final D initialValue) {
      return defaultValue(pure(initialValue));
    }

    public Self defaultValue(final ErrorCatching<Expiring<D>> initialValue) {
      this.defaultValue = initialValue;
      return (Self) this;
    }

    public Self dynamicsMapper(final ValueMapper<D> dynamicsMapper) {
      return fullDynamicsMapper(standardDynamicsMapper(dynamicsMapper));
    }

    public Self fullDynamicsMapper(final ValueMapper<ErrorCatching<Expiring<D>>> dynamicsMapper) {
      this.dynamicsMapper = dynamicsMapper;
      return (Self) this;
    }

    public Self notSaved() {
      assertSet("default value", defaultValue);
      return inconBehavior(notSaving(defaultValue));
    }

    public Self saved() {
      assertSet("name", name);
      assertSet("default value", defaultValue);
      assertSet("dynamics mapper", dynamicsMapper);
      return inconBehavior(serializing(name, defaultValue, dynamicsMapper));
    }

    public Self inconBehavior(final InconBehavior<ErrorCatching<Expiring<D>>> inconBehavior) {
      this.inconBehavior = inconBehavior;
      return (Self) this;
    }

    public Self effectTrait(final EffectTrait<DynamicsEffect<D>> effectTrait) {
      this.effectTrait = effectTrait;
      return (Self) this;
    }

    public MutableResource<D> build() {
      // If no incon behavior is given, default to "saved".
      // This will error out if not enough info to save the resource is specified,
      // guiding the user towards giving that info rather than opting out of saving the resource.
      if (inconBehavior == null) saved();
      assertSet("effect trait", effectTrait);

      MutableResource<D> result = new MutableResource<>() {
        private final CellRef<DynamicsEffect<D>, Cell<D>> cell = allocate(inconBehavior, effectTrait);

        @Override
        public void emit(final DynamicsEffect<D> effect) {
          // NOTE: The strange pattern of naming effect::apply is to create a new object, identical in behavior to effect,
          //   which we can assign a more informative name without actually getting the name of effect.
          // Replacing effect::apply with effect would create a self-loop in the naming graph on effect, which isn't allowed.
          // Using Naming.getName to get effect's current name and use that when elaborating is correct but potentially slow,
          //   depending on how deep the naming graph is.
          cell.emit(Naming.name(effect::apply, "%s on %s" + Context.get().stream().map(c -> " during " + c).collect(joining()), effect, this));
        }

        @Override
        public ErrorCatching<Expiring<D>> getDynamics() {
          return cell.get().dynamics;
        }
      };
      if (name != null) {
        Naming.name(result, name);
      }
      if (MutableResourceFlags.DETECT_BUSY_CELLS) {
        result = profileEffects(result);
      }
      if (MutableResourceFlags.PROFILE_GET_DYNAMICS) {
        result = profile(result);
      }
      return result;
    }

    private void assertSet(final String name, final Object value) {
      if (value == null) throw new IllegalStateException(name + " is not set");
    }
  }

  static <D> InconBehavior<ErrorCatching<Expiring<D>>> notSaving(D initialValue) {
    return notSaving(pure(initialValue));
  }

  static <D> InconBehavior<ErrorCatching<Expiring<D>>> notSaving(ErrorCatching<Expiring<D>> initialValue) {
    return InconBehavior.of($ -> initialValue, (s, f) -> {});
  }

  static <D> InconBehavior<ErrorCatching<Expiring<D>>> serializing(String key, D defaultValue, ValueMapper<D> mapper) {
    return serializing(key, pure(defaultValue), standardDynamicsMapper(mapper));
  }

  static <D> ValueMapper<ErrorCatching<Expiring<D>>> standardDynamicsMapper(ValueMapper<D> baseMapper) {
    return new ValueMapper<>() {
      @Override
      public ValueSchema getValueSchema() {
        // Note: Both errorMessage and expiry are optional & nullable.
        return ValueSchema.ofStruct(Map.of(
                "error", ValueSchema.STRING,
                "expiry", ValueSchema.DURATION,
                "dynamics", baseMapper.getValueSchema()));
      }

      @Override
      public SerializedValue serializeValue(ErrorCatching<Expiring<D>> value) {
        return value.match(
                success -> {
                    var map = new HashMap<String, SerializedValue>();
                    map.put("dynamics", baseMapper.serializeValue(success.data()));
                    success.expiry().value().ifPresent(expiry ->
                            map.put("expiry", duration().serializeValue(expiry)));
                    return SerializedValue.of(map);
                },
                error -> SerializedValue.of(Map.of(
                        "error", SerializedValue.of(error.getMessage())
                ))
        );
      }

      @Override
      public Result<ErrorCatching<Expiring<D>>, String> deserializeValue(SerializedValue serializedValue) {
        try {
          var map = serializedValue.asMap().orElseThrow();
          if (map.containsKey("error")) {
            return Result.success(ErrorCatching.failure(new Exception(map.get("error").asString().orElseThrow())));
          } else {
            var expiry = expiry(Optional.ofNullable(map.get("expiry"))
                    .map($ -> new NullableValueMapper<>(duration()).deserializeValue($).getSuccessOrThrow()));
            var dynamics = baseMapper.deserializeValue(map.get("dynamics")).getSuccessOrThrow();
            return Result.success(ErrorCatching.success(Expiring.expiring(dynamics, expiry)));
          }
        } catch (Throwable e) {
          // TODO - we need *way* better error reporting here, but I just can't be bothered tonight.
          return Result.failure("Failed to deserialize value as a standard wrapped dynamics object.");
        }
      }
    };
  }

  static <D> InconBehavior<ErrorCatching<Expiring<D>>> serializing(String key, ErrorCatching<Expiring<D>> defaultValue, ValueMapper<ErrorCatching<Expiring<D>>> mapper) {
    return InconBehavior.of(
            incons -> incons.get(key).map($ -> mapper.deserializeValue($).getSuccessOrThrow()).orElse(defaultValue),
            (state, fincons) -> fincons.put(key, mapper.serializeValue(state)));
  }

  static <D extends Dynamics<?, D>> void set(MutableResource<D> resource, D newDynamics) {
    resource.emit(name(DynamicsMonad.effect(x -> newDynamics), "Set %s", newDynamics));
  }

  static <D extends Dynamics<?, D>> void set(MutableResource<D> resource, Expiring<D> newDynamics) {
    resource.emit(name(ErrorCatchingMonad.<Expiring<D>, Expiring<D>>map($ -> newDynamics)::apply, "Set %s", newDynamics));
  }

  /**
   * Turn on busy cell detection.
   *
   * <p>
   *     Calling this method once before constructing your model will profile effects on every resource.
   *     Profiling effects may be compute and/or memory intensive, and should not be used in production.
   * </p>
   * <p>
   *     If only a few resources are suspect, you can also call {@link Profiling#profileEffects}
   *     directly on just those resource, rather than profiling every resource.
   * </p>
   * <p>
   *     Call {@link Profiling#dump()} to see results.
   * </p>
   */
  static void detectBusyCells() {
    MutableResourceFlags.DETECT_BUSY_CELLS = true;
  }

  /**
   * Turn on profiling for all {@link MutableResource}s created by {@link MutableResource#resource}.
   * Also implies {@link MutableResource#detectBusyCells()}.
   *
   * <p>
   *     Calling this method once before constructing your model will profile virtually every {@link MutableResource}.
   *     Profiling may be compute and/or memory intensive, and should not be used in production.
   * </p>
   * <p>
   *     If only a few resources are suspect, you can also call {@link Profiling#profile}
   *     directly on just those resource, rather than profiling every resource.
   * </p>
   * <p>
   *     Call {@link Profiling#dump()} to see results.
   * </p>
   */
  static void profileAllResources() {
    MutableResourceFlags.PROFILE_GET_DYNAMICS = true;
    detectBusyCells();
  }
}

/**
 * Private global flags for configuring cell resources for debugging.
 * Flags here are meant to be set once before constructing the model,
 * and to apply to every cell that gets built.
 */
final class MutableResourceFlags {
  public static boolean DETECT_BUSY_CELLS = false;
  public static boolean PROFILE_GET_DYNAMICS = false;
}
