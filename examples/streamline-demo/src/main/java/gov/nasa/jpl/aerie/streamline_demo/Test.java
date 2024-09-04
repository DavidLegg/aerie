package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.Reactions;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.ProbabilityDistribution;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.ProbabilityDistributionFactory;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOUR;

public class Test<T> {

    {

        // For this project, I build up the randomness like so:
        var factory = new ProbabilityDistributionFactory(123);

        // The factory contains a Java RNG to use during initialization,
        // and it generates ProbabilityDistribution objects that contain the Aerie-compatible RNG to use during simulation.
        var coinFlip = factory.bernoulli(0.5);

        // Using some of the example methods defined below.

        // Every hour, flip a coin, if heads, then
        /* ... */
        onceWhenever(fixedDuration(HOUR), conditional(coinFlip, () -> { /* ... */ }));
        // Below, we say that this event happens at interval lengths normally distributed about 2 hours:
        // I guess in theory you'd want to clamp the distribution at 0... Not sure if it actually matters, though.
        /* ... */
        Supplier<Condition> trigger2 = randomDuration(factory.gaussian(2, 0.5).map(t -> Duration.roundNearest(t, HOUR)));
        // Instead of conditioning this task with a trivial always-true distribution, we can just *not* use the conditional call.
        onceWhenever(trigger2, () -> { /* ... */ });

        Resource<Discrete<Boolean>> somethingInteresting = discreteResource(false);
        Resource<Discrete<Double>> probability = discreteResource(0.2);
        // Here, we have a boolean state saying when to try,
        // combined with a probability that can change over the course of the simulation.
        /* ... */
        Supplier<Condition> trigger1 = booleanState(somethingInteresting);
        ProbabilityDistribution<Boolean> condition = factory.uniform(0, 1).map(x -> x < currentValue(probability));
        onceWhenever(trigger1, conditional(condition, () -> { /* ... */ }));

        Resource<Discrete<Integer>> p = discreteResource(12);
        Resource<Discrete<Integer>> q = discreteResource(15);
        var delta = subtractInt(p, q);
        var X = factory.bernoulli(0.1);
        ProbabilityDistribution<Boolean> XtoDelta = () -> IntStream.range(0, currentValue(delta))
                .mapToObj(i -> X.sample())
                .reduce(false, Boolean::logicalOr);

        // Finally, for a very contrived example, we have an event that triggers whenever p goes above q,
        // with a probability of happening equal to the probability that any of (p - q) bernoulli trials succeed.
        // Yes, there are far better ways to sample such a probability, but suppose you didn't know that?
        /* ... */
        Supplier<Condition> trigger = threshold(p, Comparison.greaterThan(), q);
        onceWhenever(trigger, conditional(XtoDelta, () -> { /* ... */ }));
    }

    /**
     * Variation on {@link Reactions#whenever} that waits until the trigger is false before re-running the task.
     * This avoids tight-looping when the trigger doesn't reset as a result of the triggered action.
     */
    static void onceWhenever(Supplier<Condition> trigger, Runnable task) {
        whenever(trigger, () -> {
            task.run();
            waitUntil(trigger.get().not());
        });
    }

    static Runnable conditional(ProbabilityDistribution<Boolean> condition, Runnable task) {
        return () -> {
            if (condition.sample()) {
                task.run();
            }
        };
    }

    static Condition after(Duration time) {
        var absoluteTargetTime = currentTime().plus(time);
        return (positive, atEarliest, atLatest) ->
                Optional.of(Duration.max(absoluteTargetTime.minus(currentTime()), atEarliest))
                        .filter(atLatest::noShorterThan);
    }

    static Supplier<Condition> fixedDuration(Duration time) {
        return () -> after(time);
    }

    static Supplier<Condition> randomDuration(ProbabilityDistribution<Duration> durationDistribution) {
        return () -> after(durationDistribution.sample());
    }

    static Supplier<Condition> booleanState(Resource<Discrete<Boolean>> trigger) {
        return constant$(when(trigger));
    }

    static <T> Supplier<Condition> stateEquals(Resource<Discrete<T>> state, T value) {
        return stateEquals(state, constant(value));
    }

    static <T> Supplier<Condition> stateEquals(Resource<Discrete<T>> state1, Resource<Discrete<T>> state2) {
        return constant$(when(DiscreteResources.equals(state1, state2)));
    }

    static <T extends Comparable<T>> Supplier<Condition> threshold(Resource<Discrete<T>> state1, Comparison<T> comparison, Resource<Discrete<T>> state2) {
        return constant$(when(map(state1, state2, comparison::test)));
    }

    interface Comparison<T> extends BiPredicate<T, T> {
        static <T extends Comparable<T>> Comparison<T> lessThan() {return (a, b) -> a.compareTo(b) < 0;}
        static <T extends Comparable<T>> Comparison<T> lessThanOrEquals() {return (a, b) -> a.compareTo(b) <= 0;}
        static <T extends Comparable<T>> Comparison<T> greaterThan() {return (a, b) -> a.compareTo(b) > 0;}
        static <T extends Comparable<T>> Comparison<T> greaterThanOrEquals() {return (a, b) -> a.compareTo(b) >= 0;}
        static <T extends Comparable<T>> Comparison<T> equals() {return (a, b) -> a.compareTo(b) == 0;}
        static <T extends Comparable<T>> Comparison<T> notEquals() {return (a, b) -> a.compareTo(b) != 0;}
    }

    static <T> Supplier<T> constant$(T value) {
        return () -> value;
    }
}
