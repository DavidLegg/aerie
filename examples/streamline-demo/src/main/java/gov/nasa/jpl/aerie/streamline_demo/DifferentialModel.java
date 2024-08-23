package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentialEquations;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$double;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;

public class DifferentialModel {
    public Resource<Linear> simpleHarmonicOscillator;

    public MutableResource<Discrete<Double>> target;
    public MutableResource<Discrete<Double>> targetedOscillatorDampingRatio;
    public Resource<Linear> targetedOscillator;

    public DifferentialModel(Registrar registrar, Configuration config) {
        simpleHarmonicOscillator = DifferentialEquations.rungeKutta4(
                (t, x) -> new double[] { x[1], - config.oscillatorFrequencyHz * config.oscillatorFrequencyHz * x[0] },
                new double[] { 1, 0 },
                Duration.of(config.oscillatorTimeStepMillis, MILLISECONDS)).getFirst();

        registrar.real("differential/simpleHarmonicOscillator", simpleHarmonicOscillator);

        target = discreteResource(0.0);
        targetedOscillatorDampingRatio = discreteResource(0.0);
        targetedOscillator = DifferentialEquations.rungeKutta4(
                (t, x) -> new double[] {
                        x[1],
                        - config.oscillatorFrequencyHz * config.oscillatorFrequencyHz * (x[0] - currentValue(target))
                        - 2 * currentValue(targetedOscillatorDampingRatio) * config.oscillatorFrequencyHz * x[1]
                },
                new double[] { 0, 0 },
                Duration.of(config.oscillatorTimeStepMillis, MILLISECONDS)).getFirst();

        registrar.discrete("differential/target", target, $double());
        registrar.discrete("differential/targetedOscillatorDampingRatio", targetedOscillatorDampingRatio, $double());
        registrar.real("differential/targetedOscillator", targetedOscillator);
    }
}
