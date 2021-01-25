package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.CompoundCondition;
import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/* package-local */
final class ReactionContext<$Schema, $Timeline extends $Schema>
    implements Context<$Schema>
{
  private final TaskHandle<$Timeline> handle;
  private Scheduler<$Timeline> scheduler;
  private Optional<History<$Timeline>> history = Optional.empty();

  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs;
  private int nextBreadcrumbIndex = 0;

  public ReactionContext(
      final List<ActivityBreadcrumb<$Timeline>> breadcrumbs,
      final Scheduler<$Timeline> scheduler,
      final TaskHandle<$Timeline> handle)
  {
    this.breadcrumbs = Objects.requireNonNull(breadcrumbs);
    this.scheduler = scheduler;
    this.handle = handle;

    readvance();
  }

  @Override
  public History<$Timeline> now() {
    return this.history.orElseGet(this.scheduler::now);
  }

  @Override
  public <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
    if (this.history.isEmpty()) {
      // We're running normally.
      this.scheduler.emit(event, query);
    } else {
      // TODO: Avoid leaving garbage behind -- find some way to remove regenerated events
      //   on dead-end branches when references to it disappear.
      this.history = this.history.map(now -> now.emit(event, query));
    }
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    if (this.history.isEmpty()) {
      // We're running normally.
      final var id = this.scheduler.spawn(type, arguments);

      this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
      this.nextBreadcrumbIndex += 1;

      return id;
    } else {
      return respawn();
    }
  }

  @Override
  public String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    if (this.history.isEmpty()) {
      // We're running normally.
      final var id = this.scheduler.defer(duration, type, arguments);

      this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
      this.nextBreadcrumbIndex += 1;

      return id;
    } else {
      return respawn();
    }
  }

  @Override
  public void delay(final Duration duration) {
    if (this.history.isEmpty()) {
      // We're running normally.
      this.scheduler = this.handle.yield(TaskStatus.delayed(duration));

      this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(this.scheduler.now()));
      this.nextBreadcrumbIndex += 1;
    } else {
      readvance();
    }
  }

  @Override
  public void waitFor(final String id) {
    if (this.history.isEmpty()) {
      // We're running normally.
      this.scheduler = this.handle.yield(TaskStatus.awaiting(id));

      this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(this.scheduler.now()));
      this.nextBreadcrumbIndex += 1;
    } else {
      readvance();
    }
  }

  @Override
  public void waitUntil(final CompoundCondition<?> condition) {
    if (this.history.isEmpty()) {
      // We're running normally.

      // SAFETY: All objects accessible within a single adaptation instance have the same brand.
      @SuppressWarnings("unchecked")
      final var brandedCondition = (CompoundCondition<$Schema>) condition;
      this.scheduler = this.handle.yield(TaskStatus.awaiting(brandedCondition));

      this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(this.scheduler.now()));
      this.nextBreadcrumbIndex += 1;
    } else {
      readvance();
    }
  }

  private void readvance() {
    if (this.nextBreadcrumbIndex >= this.breadcrumbs.size() - 1) {
      // We've just now caught up.
      this.history = Optional.empty();
      this.nextBreadcrumbIndex += 1;
    } else {
      // We're still behind -- jump to the next breadcrumb.
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex);
      this.nextBreadcrumbIndex += 1;

      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new Error("Expected Advance breadcrumb; got " + breadcrumb.getClass().getName());
      }

      this.history = Optional.of(((ActivityBreadcrumb.Advance<$Timeline>) breadcrumb).next);
    }
  }

  private String respawn() {
    if (this.nextBreadcrumbIndex >= this.breadcrumbs.size() - 1) {
      // We've just now caught up.
      throw new Error("Expected a Spawn breadcrumb while replaying; found none.");
    } else {
      // We're still behind -- jump to the next breadcrumb.
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex);
      this.nextBreadcrumbIndex += 1;

      if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
        throw new Error("Expected Spawn breadcrumb; got " + breadcrumb.getClass().getName());
      }

      return ((ActivityBreadcrumb.Spawn<$Timeline>) breadcrumb).activityId;
    }
  }
}
