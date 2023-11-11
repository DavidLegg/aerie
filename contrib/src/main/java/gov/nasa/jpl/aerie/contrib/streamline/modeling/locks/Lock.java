package gov.nasa.jpl.aerie.contrib.streamline.modeling.locks;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.contextualized;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public class Lock<P extends Comparable<? super P>> {
  private final CellResource<Discrete<Optional<LockState<Pair<P, Duration>>>>> cell;
  private final Resource<Discrete<Boolean>> unlocked;

  public Lock() {
    cell = cellResource(discrete(Optional.empty()));
    unlocked = map(cell, Optional::isEmpty);
  }

  /**
   * Block until this lock can be acquired.
   * If lock is already locked, wait until it's unlocked.
   * If several requests to acquire the lock are made simultaneously,
   * the highest priority request is granted first.
   * Equal priority requests are granted first-come, first-served.
   * Simultaneous equal-priority requests cause an error.
   * This repeats as many times as necessary to satisfy all requests.
   *
   * @param priority  Indicates the priority of this request.
   *                  Higher priority requests are granted first.
   * @return An AcquiredLock object which can be used to release the lock later.
   */
  public AcquiredLock acquire(P priority) {
    // Use the pair (priority, -time) to grant higher priority locks first,
    // and grant equal-priority requests first-come, first-served.
    var lockState = new LockState<>(Pair.of(priority, currentTime().times(-1)), UUID.randomUUID());
    // Use a sub-task to capture lockState, even if this method was called from a replaying task.
    call(replaying(() -> acquire(lockState)));
    // Read lockState out of the cell, in case this method was called from a replaying task.
    // That way, we'll read the UUID that was actually set, not one generated on a replay.
    return new AcquiredLock(getLockedState());
  }

  private void acquire(LockState<Pair<P, Duration>> lockState) {
    // Since we expect to try to acquire locks a relatively small number of times,
    // use a loop instead of a trampoline to avoid task-creation overhead.
    while (true) {
      // Use an if-unlocked instead of a wait-until-unlocked here, to reduce simulation engine cycles
      // in the common case that the lock is free immediately.
      if (currentValue(unlocked)) {
        // cell is unlocked, try to lock it
        // Effect will acquire the lock iff cell is unlocked, or locked with a lower priority
        // If there are multiple acquire effects, highest priority wins.
        // If there's a tie for highest priority, that'll show up as conflicting effects.
        cell.emit("Request to acquire lock (priority %s)".formatted(lockState.priority),
                  effect(d$ -> Optional.of(
                      d$.filter(currentLock -> currentLock.priority.compareTo(lockState.priority) >= 0).orElse(lockState))));
        // Commit so we can observe concurrent requests
        delay(ZERO);
        // Value of the cell after commit is the granted request
        // Since we submitted a request, it can't be unlocked.
        if (getLockedState().id.equals(lockState.id)) {
          // We acquired the lock. Exit immediately.
          return;
        }
        // We failed to acquire the lock. Fall through to the wait-until-unlocked loop.
      }
      // It is locked. Wait for it to unlock and try again to acquire it.
      waitUntil(when(unlocked));
    }
  }

  private LockState<Pair<P, Duration>> getLockedState() {
    return currentValue(cell).orElseThrow(() -> new IllegalStateException("Lock suffered an internal error."));
  }

  /**
   * Acquire and hold this lock while running action.
   * This guarantees the lock will be released if the task completes successfully.
   *
   * @see Lock#acquire
   */
  public void using(P priority, Runnable action) {
    using(priority, () -> { action.run(); return Unit.UNIT; });
  }

  /**
   * Acquire and hold this lock while running action.
   * This guarantees the lock will be released if the task completes successfully.
   *
   * @see Lock#acquire
   */
  public <R> R using(P priority, Supplier<R> action) {
    var acquiredLock = acquire(priority);
    R result = action.get();
    acquiredLock.release();
    return result;
  }

  private record LockState<P extends Comparable<? super P>>(P priority, UUID id) {}
  public final class AcquiredLock {
    private final LockState<Pair<P, Duration>> expectedLockState;

    private AcquiredLock(final LockState<Pair<P, Duration>> expectedLockState) {
      this.expectedLockState = expectedLockState;
    }

    /**
     * Release the lock that was acquired when this object was created.
     *
     * <p>
     *   This method should be called exactly once.
     * </p>
     */
    public void release() {
      // Check that we're unlocking from the expected state
      // This detects if someone tries to release the lock multiple times.
      boolean isExpected = currentValue(cell).map(expectedLockState::equals).orElse(false);
      if (!isExpected) {
        throw new IllegalStateException("Lock has already been released!");
      }
      cell.emit("Release lock", effect(d -> Optional.empty()));
    }
  }
}
