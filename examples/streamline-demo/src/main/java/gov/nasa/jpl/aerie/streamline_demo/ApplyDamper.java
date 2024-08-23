package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.decrease;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.increase;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

@ActivityType("ApplyDamper")
public class ApplyDamper {
    @Export.Parameter
    public double strength = 0.5;

    @Export.Parameter
    public double seconds = 5;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        increase(mission.differentialModel.targetedOscillatorDampingRatio, strength);
        delay(Duration.roundNearest(seconds, SECONDS));
        decrease(mission.differentialModel.targetedOscillatorDampingRatio, strength);
    }
}
