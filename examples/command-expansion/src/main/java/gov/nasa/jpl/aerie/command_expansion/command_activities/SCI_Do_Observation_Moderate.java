package gov.nasa.jpl.aerie.command_expansion.command_activities;

import gov.nasa.jpl.aerie.command_expansion.generated.ActivityActions;
import gov.nasa.jpl.aerie.command_expansion.model.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import java.util.List;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

@ActivityType("SCI_Do_Observation_Moderate")
public class SCI_Do_Observation_Moderate implements Command {
    @Export.Parameter
    public int duration = 3600;

    @Override
    public List<Object> args() {
        return List.of(duration);
    }

    @ActivityType.EffectModel
    @Override
    public void run(Mission mission) {
        // Only duration is modeled
        delay(duration, SECONDS);
    }

    @Override
    public void call(Mission mission) {
        ActivityActions.call(mission, this);
    }
}
