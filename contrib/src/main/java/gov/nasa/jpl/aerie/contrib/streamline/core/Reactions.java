package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Condition;

import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.replaying;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

public final class Reactions {
  private Reactions() {}

  public static void whenever(Resource<Discrete<Boolean>> conditionResource, Runnable action) {
    whenever(when(conditionResource), action);
  }

  public static void whenever(Condition condition, Runnable action) {
    whenever(() -> condition, action);
  }

  public static void whenever(Supplier<Condition> trigger, Runnable action) {
    final Condition condition = trigger.get();
    // Use replaying tasks to avoid threading overhead.
    spawn(replaying(() -> {
      waitUntil(condition);
      action.run();
      // Trampoline off this task to avoid replaying.
      whenever(trigger, action);
    }));
  }
}
