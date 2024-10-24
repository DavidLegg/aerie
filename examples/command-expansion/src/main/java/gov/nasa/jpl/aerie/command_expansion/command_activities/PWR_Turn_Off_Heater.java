package gov.nasa.jpl.aerie.command_expansion.command_activities;

import gov.nasa.jpl.aerie.command_expansion.generated.ActivityActions;
import gov.nasa.jpl.aerie.command_expansion.model.Mission;
import gov.nasa.jpl.aerie.command_expansion.model.PowerModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import java.util.List;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;

@ActivityType("PWR_Turn_Off_Heater")
public class PWR_Turn_Off_Heater implements Command {
    @Export.Parameter
    public boolean fullShutdown = false;

    @ActivityType.EffectModel
    @Override
    public void run(Mission mission) {
        set(mission.power.heater, fullShutdown ? PowerModel.HeaterState.OFF : PowerModel.HeaterState.STANDBY);
    }

    @Override
    public List<Object> args() {
        return List.of(fullShutdown);
    }

    @Override
    public void call(Mission mission) {
        ActivityActions.call(mission, this);
    }
}
