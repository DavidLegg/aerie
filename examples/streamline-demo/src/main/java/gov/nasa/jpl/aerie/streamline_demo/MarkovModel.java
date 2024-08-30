package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.MarkovProcess;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.ProbabilityDistributionFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$enum;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.every;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources.approximateAsLinear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources.asUnstructured;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredResourceApplicative.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock.pausedStopwatch;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClockEffects.pause;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClockEffects.start;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClockResources.asLinear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

public class MarkovModel {
    public final Resource<Discrete<MMState>> state;
    public final Map<MMState, Resource<VariableClock>> cumulativeTimeInState;
    public final Map<MMState, Resource<Unstructured<Double>>> proportionalTimeInState;

    public MarkovModel(Registrar registrar, Configuration config) {
        // NOTE - I'm constructing the distribution factory right here, and immediately using it.
        // In a "real" model, you'd want to construct one factory at the top level
        // and give a split() of that to each sub-model, to ensure deterministic-but-(pseudo)independent randomness.
        var factory = new ProbabilityDistributionFactory(config.seed);
        var process = new MarkovProcess<>(MMState.A, new TreeMap<>(Map.of(
                MMState.A, List.of(Pair.of(MMState.A, 0.6), Pair.of(MMState.B, 0.4)),
                MMState.B, List.of(Pair.of(MMState.A, 0.7), Pair.of(MMState.B, 0.3)))),
                factory);
        state = process.state;
        registrar.discrete("markov/state", state, $enum(MMState.class));

        // Here, I'm driving the process with a simple periodic transition.
        // Of course, you could have more involved conditions that drive the process more or less frequently.
        // every(Duration.MINUTE, process::transition);
        // The version above will work if we just want to drive the markov process itself.
        // The version below allows us to also use the reported transition to do something else:
         every(Duration.MINUTE, () -> {
             var transition = process.transition();
             LOGGER.info("Markov Transition: %s -> %s", transition.startState(), transition.endState());
         });

        // Alternatively, we could monitor the state resource itself to react to specific changes.
        // For example, here's one way we might record the cumulative time we spend in each state.
        cumulativeTimeInState = new HashMap<>();
        for (var mmState : MMState.values()) {
            var timer = resource(pausedStopwatch());
            var processInMMState = DiscreteResources.equals(state, constant(mmState));
            whenever(processInMMState, () -> {
                start(timer);
                waitUntil(when(not(processInMMState)));
                pause(timer);
            });
            cumulativeTimeInState.put(mmState, timer);
            registrar.real("markov/cumulativeHours/" + mmState, asLinear(timer, Duration.HOURS));
        }

        var totalTime = clock();
        proportionalTimeInState = new HashMap<>();
        for (var mmState : MMState.values()) {
            var proportion = map(
                    asUnstructured(cumulativeTimeInState.get(mmState)),
                    asUnstructured(totalTime),
                    Duration::ratioOver);
            proportionalTimeInState.put(mmState, proportion);
            registrar.real("markov/proportionalTime/" + mmState, approximateAsLinear(proportion));
        }
    }

    public enum MMState { A, B }

}
