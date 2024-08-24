package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentialEquations;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentialEquations.FirstOrderODEIntegrator;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$double;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentialEquations.euler;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentialEquations.rungeKutta4;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;

public class DifferentialModel {
    public Resource<Linear> simpleHarmonicOscillator;

    public MutableResource<Discrete<Double>> target;
    public MutableResource<Discrete<Double>> targetedOscillatorDampingRatio;
    public Resource<Linear> targetedOscillator;

    public DifferentialModel(Registrar registrar, Configuration config) {
        Duration timeStep1 = Duration.of(config.oscillatorTimeStepMillis, MILLISECONDS);
        simpleHarmonicOscillator = DifferentialEquations.firstOrderODE(
                config.odeIntegrator.integrator,
                (t1, x1) -> new double[]{x1[1], -config.oscillatorFrequencyHz * config.oscillatorFrequencyHz * x1[0]},
                new double[]{1, 0},
                timeStep1
        ).getFirst();

        registrar.real("differential/simpleHarmonicOscillator", simpleHarmonicOscillator);

        target = discreteResource(0.0);
        targetedOscillatorDampingRatio = discreteResource(0.0);
        Duration timeStep = Duration.of(config.oscillatorTimeStepMillis, MILLISECONDS);
        targetedOscillator = DifferentialEquations.firstOrderODE(
                config.odeIntegrator.integrator,
                (t, x) -> new double[]{
                        x[1],
                        -config.oscillatorFrequencyHz * config.oscillatorFrequencyHz * (x[0] - currentValue(target))
                                - 2 * currentValue(targetedOscillatorDampingRatio) * config.oscillatorFrequencyHz * x[1]
                },
                new double[]{0, 0},
                timeStep
        ).getFirst();

        registrar.discrete("differential/target", target, $double());
        registrar.discrete("differential/targetedOscillatorDampingRatio", targetedOscillatorDampingRatio, $double());
        registrar.real("differential/targetedOscillator", targetedOscillator);
    }

    public enum ODEIntegrator {
        RUNGE_KUTTA_4(rungeKutta4()),
        EULER(euler());

        final FirstOrderODEIntegrator integrator;

        ODEIntegrator(FirstOrderODEIntegrator integrator) {
            this.integrator = integrator;
        }
    }
}
