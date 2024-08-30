package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.AerieRandom;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.MarkovProcess;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.ProbabilityDistributionFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$enum;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.every;

public class MarkovModel {
    public final Resource<Discrete<MMState>> state;

    public MarkovModel(Registrar registrar, Configuration config) {
        // NOTE - I'm constructing the distribution factory right here, and immediately using it.
        // In a "real" model, you'd want to construct one factory at the top level
        // and give a split() of that to each sub-model, to ensure deterministic-but-(pseudo)independent randomness.
        var factory = new ProbabilityDistributionFactory(new AerieRandom(config.seed));
        var process = new MarkovProcess<>(MMState.A, Map.of(
                MMState.A, List.of(Pair.of(MMState.A, 0.6), Pair.of(MMState.B, 0.4)),
                MMState.B, List.of(Pair.of(MMState.A, 0.7), Pair.of(MMState.B, 0.3))),
                factory);

        // Here, I'm driving the process with a simple periodic transition.
        // Of course, you could have more involved conditions that drive the process more or less frequently.
        every(Duration.MINUTE, process::transition);

        this.state = process.state;
        registrar.discrete("markov/state", state, $enum(MMState.class));
    }

    public enum MMState { A, B }

}
