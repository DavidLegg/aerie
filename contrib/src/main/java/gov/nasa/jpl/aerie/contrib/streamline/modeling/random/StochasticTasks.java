package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.every;

// These methods are so small, it's arguable whether it's worth having them...

public final class StochasticTasks {
    private StochasticTasks() {}

    public static Runnable conditioned(ProbabilityDistribution<Boolean> condition, Runnable task) {
        return () -> {if (condition.sample()) task.run();};
    }

    public static void repeating(ProbabilityDistribution<Duration> period, Runnable task) {
        every(period::sample, task);
    }
}
