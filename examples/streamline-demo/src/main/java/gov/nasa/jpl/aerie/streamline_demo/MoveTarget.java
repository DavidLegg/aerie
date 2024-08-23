package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;

@ActivityType("MoveTarget")
public class MoveTarget {
    @Export.Parameter
    public double newTarget = 0.0;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        set(mission.differentialModel.target, newTarget);
    }
}
