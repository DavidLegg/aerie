package gov.nasa.jpl.aerie.e2e.procedural.scheduling.procedures;

import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.ammos.aerie.procedural.scheduling.Goal;
import gov.nasa.ammos.aerie.procedural.scheduling.annotations.SchedulingProcedure;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.DirectiveStart;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Waits 24hrs into the plan, then places `quantity` number of BiteBanana activities,
 * one every 6hrs.
 */
@SchedulingProcedure
public record DumbRecurrenceGoal(int quantity) implements Goal {
  @Override
  public void run(@NotNull final EditablePlan plan) {
    final var firstTime = Duration.hours(24);
    final var step = Duration.hours(6);

    var currentTime = firstTime;
    for (var i = 0; i < quantity; i++) {
      plan.create(
          "BiteBanana",
          new DirectiveStart.Absolute(currentTime),
          Map.of()
      );
      currentTime = currentTime.plus(step);
    }
    plan.commit();
  }
}
