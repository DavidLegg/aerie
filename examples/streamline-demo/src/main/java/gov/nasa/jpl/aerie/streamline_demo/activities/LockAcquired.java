package gov.nasa.jpl.aerie.streamline_demo.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.streamline_demo.Mission;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.inContext;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * Helper activity to display on the timeline the duration that a lock is actually held.
 */
@ActivityType("LockAcquired")
public class LockAcquired {
  @Parameter
  public Duration duration;

  @EffectModel
  public void run(Mission mission) {
    inContext(this.getClass().getSimpleName(), () -> delay(duration));
  }
}
