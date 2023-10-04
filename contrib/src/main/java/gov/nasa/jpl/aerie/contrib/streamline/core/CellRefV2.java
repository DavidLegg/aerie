package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.Labelled.Context;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;
import java.util.function.BinaryOperator;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Labelled.labelled;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class CellRefV2 {
  private CellRefV2() {}

  /**
   * Allocate a new resource with an explicitly given effect type and effect trait.
   */
  public static <D extends Dynamics<?, D>, E extends DynamicsEffect<D>> CellRef<Labelled<E>, Cell<D>> allocate(ErrorCatching<Expiring<D>> initialDynamics, EffectTrait<Labelled<E>> effectTrait) {
    return CellRef.allocate(new Cell<>(initialDynamics), new CellType<>() {
      @Override
      public EffectTrait<Labelled<E>> getEffectType() {
        return effectTrait;
      }

      @Override
      public Cell<D> duplicate(Cell<D> cell) {
        return new Cell<>(cell.initialDynamics, cell.dynamics, cell.elapsedTime);
      }

      @Override
      public void apply(Cell<D> cell, Labelled<E> effect) {
        // TODO: Should we catch errors and annotate them with label information?
        cell.initialDynamics = effect.data().apply(cell.dynamics);
        cell.dynamics = cell.initialDynamics;
        cell.elapsedTime = ZERO;
      }

      @Override
      public void step(Cell<D> cell, Duration duration) {
        // Avoid accumulated round-off error in imperfect stepping
        // by always stepping up from the initial dynamics
        cell.elapsedTime = cell.elapsedTime.plus(duration);
        cell.dynamics = ErrorCatchingMonad.map(cell.initialDynamics, d ->
            expiring(d.data().step(cell.elapsedTime), d.expiry().minus(cell.elapsedTime)));
      }
// DEBUG: Trying this without exposing expiry info to Aerie.
// We always update cells ourselves, and our notion of expiry clashes a little with Aerie's on some edge-cases.
//      @Override
//      public Optional<Duration> getExpiry(Cell<D> cell) {
//        return cell.dynamics.match(
//            expiring -> expiring.expiry().value(),
//            exception -> Optional.empty());
//      }
    });
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> noncommutingEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> {
          throw new UnsupportedOperationException(
              "Concurrent effects are not supported on this resource.");
        });
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> commutingEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> right.apply(left.apply(x)));
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> autoEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> {
      final var lrx = left.apply(right.apply(x));
      final var rlx = right.apply(left.apply(x));
      if (lrx.equals(rlx)) {
        return lrx;
      } else {
        throw new UnsupportedOperationException(
            "Detected non-commuting concurrent effects!");
      }
    });
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> resolvingConcurrencyBy(BinaryOperator<DynamicsEffect<D>> combineConcurrent) {
    return new EffectTrait<>() {
      @Override
      public Labelled<DynamicsEffect<D>> empty() {
        return labelled("No-op", x -> x);
      }

      @Override
      public Labelled<DynamicsEffect<D>> sequentially(final Labelled<DynamicsEffect<D>> prefix, final Labelled<DynamicsEffect<D>> suffix) {
        return new Labelled<>(
            x -> suffix.data().apply(prefix.data().apply(x)),
            "(%s) then (%s)".formatted(prefix.name(), suffix.name()),
            new Context("Combining Sequential Effects", List.of(prefix.context(), suffix.context())));
      }

      @Override
      public Labelled<DynamicsEffect<D>> concurrently(final Labelled<DynamicsEffect<D>> left, final Labelled<DynamicsEffect<D>> right) {
        var context = new Context("Combining Concurrent Effects", List.of(left.context(), right.context()));
        try {
          final DynamicsEffect<D> combined = combineConcurrent.apply(left.data(), right.data());
          return new Labelled<>(
              x -> {
                try {
                  return combined.apply(x);
                } catch (Exception e) {
                  return failure(e);
                }
              },
              "(%s) and (%s)".formatted(left.name(), right.name()),
              context);
        } catch (Throwable e) {
          return new Labelled<>(
              $ -> failure(e),
              "Failed to combine concurrent effects: (%s) and (%s)".formatted(left.name(), right.name()),
              context);
        }
      }
    };
  }

  public static class Cell<D> {
    public ErrorCatching<Expiring<D>> initialDynamics;
    public ErrorCatching<Expiring<D>> dynamics;
    public Duration elapsedTime;

    public Cell(ErrorCatching<Expiring<D>> dynamics) {
      this(dynamics, dynamics, ZERO);
    }

    public Cell(ErrorCatching<Expiring<D>> initialDynamics, ErrorCatching<Expiring<D>> dynamics, Duration elapsedTime) {
      this.initialDynamics = initialDynamics;
      this.dynamics = dynamics;
      this.elapsedTime = elapsedTime;
    }
  }
}
