package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.framework.Scoped.EmptyDynamicCellException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

/**
 * Utility methods for {@link Resource}s.
 */
public final class Resources {
  private Resources() {}

  private static CellResource<ClockDynamics> CLOCK = CellResource.cellResource(new ClockDynamics(ZERO));
  public static Duration currentTime() {
    try {
      return currentValue(CLOCK);
    } catch (EmptyDynamicCellException e) {
      // If we're running unit tests, several simulations can happen without reloading the Resources class.
      // In that case, we'll have discarded the clock cell we were using, and get the above exception.
      // REVIEW: Is there a cleaner way to make sure this cell gets (re-)initialized?
      CLOCK = CellResource.cellResource(new ClockDynamics(ZERO));
      return currentValue(CLOCK);
    }
  }

  public static <V, D extends Dynamics<V, D>> V currentValue(Resource<D> resource) {
    return resource.getDynamics().data().extract();
  }

  public static <D extends Dynamics<?, D>> Condition dynamicsChange(Resource<D> resource) {
    final Expiring<D> startingDynamics = resource.getDynamics();
    final Duration startTime = currentTime();
    return (positive, atEarliest, atLatest) -> {
      Expiring<D> currentDynamics = resource.getDynamics();
      boolean haveChanged = !currentDynamics.data().equals(
          startingDynamics.data().step(currentTime().minus(startTime)));

      return positive == haveChanged
          ? Optional.of(atEarliest)
          : positive
            ? currentDynamics.expiry().value().filter(atLatest::noShorterThan)
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
    var cell = allocate(resource.getDynamics());
    // TODO: Implement an efficient repeating task and use it here.
    spawn(() -> {
      while (true) {
        waitUntil(dynamicsChange(resource));
        cell.emit($ -> resource.getDynamics());
      }
    });
    return () -> cell.get().dynamics;
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
    var cell = allocate(discrete(Unit.UNIT));
    // TODO: Implement an efficient repeating task and use it here.
    spawn(() -> {
      while (true) {
        waitUntil(dynamicsChange(resource));
        cell.emit($ -> $);
      }
    });
    return () -> {
      cell.get();
      return resource.getDynamics();
    };
  }

  public static <D extends Dynamics<?, D>> Resource<D> shift(Resource<D> resource, Duration interval, D initialDynamics) {
    var cell = allocate(initialDynamics);
    // TODO: Implement an efficient repeating task and use it here.
    spawn(() -> {
      while (true) {
        spawn(replaying(() -> {
          var dynamics = resource.getDynamics();
          delay(interval);
          cell.emit($ -> dynamics);
        }));
        waitUntil(dynamicsChange(resource));
      }
    });
    return () -> cell.get().dynamics;
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
