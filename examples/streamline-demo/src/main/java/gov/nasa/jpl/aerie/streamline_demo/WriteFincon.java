package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.InitialConditionManager;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;

@ActivityType("WriteFincon")
public class WriteFincon {
    @ActivityType.EffectModel
    public void run(Mission mission) {
        InitialConditionManager.writeFincon();
    }
}
