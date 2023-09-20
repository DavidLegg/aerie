package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.framework.Scoped.EmptyDynamicCellException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

/**
 * Utility methods for {@link Resource}s.
 */
public final class Resources {
  private Resources() {}

  /**
   * Ensure that Resources are initialized.
   *
   * <p>
   *   This method needs to be called during simulation initialization.
   *   This method is idempotent; calling it multiple times is the same as calling it once.
   * </p>
   */
  // TODO: Consider adding this somewhere in the Registrar class, since that'll be used during initialization.
  public static void init() {
    currentTime();
  }

  private static CellResource<ClockDynamics> CLOCK = cellResource(new ClockDynamics(ZERO));
  public static Duration currentTime() {
    try {
      return currentValue(CLOCK);
    } catch (EmptyDynamicCellException | IllegalArgumentException e) {
      // If we're running unit tests, several simulations can happen without reloading the Resources class.
      // In that case, we'll have discarded the clock cell we were using, and get the above exception.
      // REVIEW: Is there a cleaner way to make sure this cell gets (re-)initialized?
      CLOCK = cellResource(new ClockDynamics(ZERO));
      return currentValue(CLOCK);
    }
  }

  public static <D> D currentData(Resource<D> resource) {
    return resource.getDynamics().getOrThrow().data();
  }

  public static <V, D extends Dynamics<V, D>> V currentValue(Resource<D> resource) {
    return currentData(resource).extract();
  }

  public static <D extends Dynamics<?, D>> Condition dynamicsChange(Resource<D> resource) {
    final var startingDynamics = resource.getDynamics();
    final Duration startTime = currentTime();
    return (positive, atEarliest, atLatest) -> {
      var currentDynamics = resource.getDynamics();
      boolean haveChanged = startingDynamics.match(
          start -> currentDynamics.match(
              current -> !current.data().equals(start.data().step(currentTime().minus(startTime))),
              ignored -> true),
          startException -> currentDynamics.match(
              ignored -> true,
              currentException -> !startException.equals(currentException)));

      return positive == haveChanged
          ? Optional.of(atEarliest)
          : positive
            ? currentDynamics.match(
                expiring -> expiring.expiry().value().filter(atLatest::noShorterThan),
                exception -> Optional.empty())
            : Optional.empty();
    };
  }

  /**
   * Cache this resource in a cell.
   *
   * <p>
   *   Updates the cell when resource changes dynamics.
   *   This can be used to isolate a resource from effects
   *   which don't change the dynamics, so Aerie samples that
   *   resource only when strictly necessary.
   * </p>
   * <p>
   *   This introduces a small delay in deriving values.
   *   Specifically, the cached version of a resource changes two
   *   simulation engine cycles after its uncached version.
   *   It will show up as the same instant in the results,
   *   but beware that it could be momentarily out-of-sync
   *   with its sources during simulation.
   * </p>
   */
  public static <D extends Dynamics<?, D>> Resource<D> cache(Resource<D> resource) {
    var cell = cellResource(resource.getDynamics());
    wheneverDynamicsChange(resource, newDynamics -> cell.emit($ -> newDynamics));
    return cell;
  }

  /**
   * Signal discrete changes in this resource's dynamics.
   *
   * <p>
   *   For Aerie's resource sampling to work correctly,
   *   there must be an effect every time a resource changes dynamics.
   *   For most derived resources, this happens automatically.
   *   For some derivations, though, continuous changes in the source state
   *   can cause discrete changes in the result.
   *   For example, imagine a continuous numeric resource R,
   *   and a derived resource S := "R > 0".
   *   If R changes continuously from positive to negative,
   *   then S changes discretely from true to false, *without* an effect.
   *   If used directly, Aerie would not re-sample S at this time.
   *   This method emits a trivial effect when this happens so that S
   *   *would* be resampled correctly.
   * </p>
   * <p>
   *   Unlike {@link Resources#cache}, this method does *not* introduce
   *   a delay between the source and derived resources.
   *   Signalling resources use a cell "in parallel" rather than "in series"
   *   with the derivation process, thereby avoiding the delay.
   *   Like regular derived resources, signalling resources calculate their value
   *   through the derivation every time they are sampled.
   * </p>
   */
  // REVIEW: Suggestion from Jonathan Castello to remove this method
  // in favor of allowing resources to report expiry information directly.
  // This would be cleaner and potentially more performant.
  public static <D extends Dynamics<?, D>> Resource<D> signalling(Resource<D> resource) {
    var cell = cellResource(discrete(Unit.UNIT));
    wheneverDynamicsChange(resource, ignored -> cell.emit($ -> $));
    return () -> {
      cell.getDynamics();
      return resource.getDynamics();
    };
  }

  public static <D extends Dynamics<?, D>> Resource<D> shift(Resource<D> resource, Duration interval, D initialDynamics) {
    var cell = cellResource(initialDynamics);
    delayedSet(cell, resource.getDynamics(), interval);
    wheneverDynamicsChange(resource, newDynamics ->
        delayedSet(cell, newDynamics, interval));
    return cell;
  }

  private static <D extends Dynamics<?, D>> void delayedSet(
      CellResource<D> cell, ErrorCatching<Expiring<D>> newDynamics, Duration interval)
  {
    spawn(replaying(() -> {
      delay(interval);
      cell.emit($ -> newDynamics);
    }));
  }

  private record ClockDynamics(Duration time) implements Dynamics<Duration, ClockDynamics> {
    @Override
    public Duration extract() {
      return time;
    }

    @Override
    public ClockDynamics step(final Duration t) {
      return new ClockDynamics(t.plus(time));
    }
  }
}
