package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;

public class MarkovProcess<T> {
    // Exposing the state directly as a MutableResource allows the consumer to make non-random transitions,
    // e.g. to support resetting the process.
    public final MutableResource<Discrete<T>> state;
    private final Map<T, ProbabilityDistribution<T>> transitionDistributions;

    public MarkovProcess(T initialState, Map<T, Collection<Pair<T, Double>>> weightedTransitionTable, ProbabilityDistributionFactory distributionFactory) {
        state = discreteResource(initialState);
        transitionDistributions = new HashMap<>();
        weightedTransitionTable.forEach((state, transitions) ->
                transitionDistributions.put(state, distributionFactory.weighted(transitions)));
    }

    public MarkovProcessTransition<T> transition() {
        var startState = currentValue(state);
        var endState = transitionDistributions.get(startState).sample();
        set(state, endState);
        return new MarkovProcessTransition<>(startState, endState);
    }
    public record MarkovProcessTransition<T>(T startState, T endState) {}
}
