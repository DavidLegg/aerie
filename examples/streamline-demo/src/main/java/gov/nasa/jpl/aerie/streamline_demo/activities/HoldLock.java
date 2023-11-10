package gov.nasa.jpl.aerie.streamline_demo.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.streamline_demo.Mission;
import gov.nasa.jpl.aerie.streamline_demo.models.LockModel.Priority;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.inContext;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.decrement;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.increment;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("HoldLock")
public class HoldLock {
  @Parameter
  public String name;

  @Parameter
  public Priority priority;

  @Parameter
  public Duration duration;

  @EffectModel
  public void run(Mission mission) {
    inContext(this.getClass().getSimpleName() + " " + name, () -> {
      increment(mission.lockModel.pendingLockRequests);
      mission.lockModel.lock.using(priority, () -> {
        decrement(mission.lockModel.pendingLockRequests);
        set(mission.lockModel.lockHolder, name);
        set(mission.lockModel.lockHolderPriority, priority);
        delay(duration);
        set(mission.lockModel.lockHolderPriority, null);
        set(mission.lockModel.lockHolder, "");
      });
    });
  }
}
