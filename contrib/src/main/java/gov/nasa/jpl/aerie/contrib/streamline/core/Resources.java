package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Optional;
import java.util.function.Consumer;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock.clock;
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
  public static void init() {
    currentTime();
  }

  private static Resource<Clock> CLOCK = cellResource(clock(ZERO));
  public static Duration currentTime() {
    try {
      return currentValue(CLOCK);
    } catch (Scoped.EmptyDynamicCellException | IllegalArgumentException e) {
      // If we're running unit tests, several simulations can happen without reloading the Resources class.
      // In that case, we'll have discarded the clock resource we were using, and get the above exception.
      // REVIEW: Is there a cleaner way to make sure this resource gets (re-)initialized?
      CLOCK = cellResource(clock(ZERO));
      return currentValue(CLOCK);
    }
  }

  public static <D> D currentData(Resource<D> resource) {
    return data(resource.getDynamics());
  }

  public static <D> D currentData(Resource<D> resource, D dynamicsIfError) {
    return data(resource.getDynamics(), dynamicsIfError);
  }

  public static <V, D extends Dynamics<V, D>> V currentValue(Resource<D> resource) {
    return value(resource.getDynamics());
  }

  public static <V, D extends Dynamics<V, D>> V currentValue(Resource<D> resource, V valueIfError) {
    return value(resource.getDynamics(), valueIfError);
  }

  public static <D> D data(ErrorCatching<Expiring<D>> dynamics) {
    return dynamics.getOrThrow().data();
  }

  public static <D> D data(ErrorCatching<Expiring<D>> dynamics, D dynamicsIfError) {
    return dynamics.match(Expiring::data, error -> dynamicsIfError);
  }

  public static <V, D extends Dynamics<V, D>> V value(ErrorCatching<Expiring<D>> dynamics) {
    return data(dynamics).extract();
  }

  public static <V, D extends Dynamics<V, D>> V value(ErrorCatching<Expiring<D>> dynamics, V valueIfError) {
    return dynamics.match(result -> result.data().extract(), error -> valueIfError);
  }

  /**
   * Condition that's triggered when the dynamics on resource change in a way
   * that's different from just evolving with time.
   * This can be due to effects on a cell used by this resource,
   * or by some part of the derivation expiring.
   */
  public static <D extends Dynamics<?, D>> Condition dynamicsChange(Resource<D> resource) {
    final var startingDynamics = resource.getDynamics();
    final Duration startTime = currentTime();
    Condition result = (positive, atEarliest, atLatest) -> {
      var currentDynamics = resource.getDynamics();
      boolean haveChanged = startingDynamics.match(
          start -> currentDynamics.match(
              current -> !current.data().equals(start.data().step(currentTime().minus(startTime))),
              ignored -> true),
          startException -> currentDynamics.match(
              ignored -> true,
              // Use semantic comparison for exceptions, since derivation can generate the exception each invocation.
              currentException -> !equivalentExceptions(startException, currentException)));

      return positive == haveChanged
          ? Optional.of(atEarliest)
          : positive
            ? currentDynamics.match(
                expiring -> expiring.expiry().value().filter(atLatest::noShorterThan),
                exception -> Optional.empty())
            : Optional.empty();
    };
    Naming.registerName(result, "Dynamics Change (%s)", resource);
    return result;
  }

  /**
   * A weaker form of {@link Resources#dynamicsChange},
   * which doesn't attempt to compare dynamics.
   * <p>
   * This Condition is less robust, and may trigger spuriously.
   * When used in a reaction loop like {@link Reactions#wheneverUpdates(Resource, Consumer)},
   * there is a known 1-tick "blindspot" after updates fires; if two updates happen to resource
   * in back-to-back simulation ticks at the same time, only the first triggers the reaction loop.
   * One way to handle this blindspot is with spawn and delay, like this:
   * <pre>
   * wheneverUpdates(resource, () -> {
   *   // Handle the immediate update, if desired
   *   spawn(replaying(() -> {
   *     delay(ZERO);
   *     // Handle an update 1 tick after the update that triggered this loop, if any happened.
   *   }));
   * });
   * </pre>
   * </p>
   * <p>
   * However, this condition doesn't depend on dynamics having a well-behaved equals method.
   * </p>
   */
  public static Condition updates(Resource<?> resource) {
    var result = new Condition() {
      private boolean first = true;

      @Override
      public Optional<Duration> nextSatisfied(
          final boolean positive,
          final Duration atEarliest,
          final Duration atLatest)
      {
        // Get resource to subscribe this condition to resource's cells
        var dynamics = resource.getDynamics();
        if (first) {
          first = false;
          return dynamics.match(Expiring::expiry, e -> NEVER).value().filter(atLatest::noShorterThan);
        } else {
          return Optional.of(atEarliest);
        }
      }
    };
    Naming.registerName(result, "Updates (%s)", resource);
    return result;
  }

  public static Condition expires(Resource<?> resource) {
    Condition result = (positive, atEarliest, atLatest) -> resource.getDynamics().match(
        expiring -> expiring.expiry().value().filter(atLatest::noShorterThan).map(t -> Duration.max(t, atEarliest)),
        error -> Optional.empty());
    Naming.registerName(result, "Expires (%s)", resource);
    return result;
  }

  // TODO: Should this be moved somewhere else?
  /**
   * Tests if two exceptions are equivalent from the point of view of resource values.
   * Two exceptions are equivalent if they have the same type and message.
   */
  public static boolean equivalentExceptions(Throwable startException, Throwable currentException) {
    return startException.getClass().equals(currentException.getClass())
           && startException.getMessage().equals(currentException.getMessage());
  }

  /**
   * Cache this resource in a resource.
   *
   * <p>
   *   Updates the resource when resource changes dynamics.
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
    Naming.registerName(cell, "Cache (%s)", resource);
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
   *   Signalling resources use a resource "in parallel" rather than "in series"
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
    Resource<D> result = () -> {
      cell.getDynamics();
      return resource.getDynamics();
    };
    Naming.registerName(result, "Signalling (%s)", resource);
    return result;
  }

  public static <D extends Dynamics<?, D>> Resource<D> shift(Resource<D> resource, Duration interval, D initialDynamics) {
    var cell = cellResource(initialDynamics);
    delayedSet(cell, resource.getDynamics(), interval);
    wheneverDynamicsChange(resource, newDynamics ->
        delayedSet(cell, newDynamics, interval));
    Naming.registerName(cell, "Shifted (%s)", resource);
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

  /**
   * Erase expiry information from a resource.
   *
   * <p>
   *   This is useful when a resource is defined through a feedback loop,
   *   to not propagate the expiry across iterations of that loop
   * </p>
   */
  public static <D> Resource<D> eraseExpiry(Resource<D> p) {
    return () -> p.getDynamics().map(e -> neverExpiring(e.data()));
  }
}
