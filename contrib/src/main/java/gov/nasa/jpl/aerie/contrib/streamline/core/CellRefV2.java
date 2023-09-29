package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;
import java.util.function.BinaryOperator;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class CellRefV2 {
  private CellRefV2() {}

  /**
   * Allocate a new resource with an explicitly given effect type and effect trait.
   */
  public static <D extends Dynamics<?, D>, E extends DynamicsEffect<D>> CellRef<E, Cell<D>> allocate(ErrorCatching<Expiring<D>> initialDynamics, EffectTrait<E> effectTrait) {
    return CellRef.allocate(new Cell<>(initialDynamics), new CellType<>() {
      @Override
      public EffectTrait<E> getEffectType() {
        return effectTrait;
      }

      @Override
      public Cell<D> duplicate(Cell<D> cell) {
        return new Cell<>(cell.initialDynamics, cell.dynamics, cell.elapsedTime);
      }

      @Override
      public void apply(Cell<D> cell, E effect) {
        cell.initialDynamics = effect.apply(cell.dynamics);
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

  public static <D extends Dynamics<?, D>> EffectTrait<DynamicsEffect<D>> noncommutingEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> {
          throw new UnsupportedOperationException(
              "Concurrent effects are not supported on this resource.");
        });
  }

  public static <D extends Dynamics<?, D>> EffectTrait<DynamicsEffect<D>> commutingEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> right.apply(left.apply(x)));
  }

  public static <D extends Dynamics<?, D>> EffectTrait<DynamicsEffect<D>> autoEffects() {
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

  public static <D extends Dynamics<?, D>> EffectTrait<DynamicsEffect<D>> resolvingConcurrencyBy(BinaryOperator<DynamicsEffect<D>> combineConcurrent) {
    return new EffectTrait<>() {
      @Override
      public DynamicsEffect<D> empty() {
        return x -> x;
      }

      @Override
      public DynamicsEffect<D> sequentially(final DynamicsEffect<D> prefix, final DynamicsEffect<D> suffix) {
        return x -> suffix.apply(prefix.apply(x));
      }

      @Override
      public DynamicsEffect<D> concurrently(final DynamicsEffect<D> left, final DynamicsEffect<D> right) {
        try {
          final DynamicsEffect<D> combined = combineConcurrent.apply(left, right);
          return x -> {
            try {
              return combined.apply(x);
            } catch (Exception e) {
              return failure(e);
            }
          };
        } catch (Exception e) {
          return $ -> failure(e);
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
