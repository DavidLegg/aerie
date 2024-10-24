package gov.nasa.jpl.aerie.command_expansion.planning_activities;

import gov.nasa.jpl.aerie.command_expansion.command_activities.PWR_Turn_Off_Heater;
import gov.nasa.jpl.aerie.command_expansion.command_activities.PWR_Turn_On_Heater;
import gov.nasa.jpl.aerie.command_expansion.expansion.Sequence;
import gov.nasa.jpl.aerie.command_expansion.expansion.TimedCommand;
import gov.nasa.jpl.aerie.command_expansion.model.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTES;

@ActivityType("Warm_Up")
public class Warm_Up {
    // Taking a page from Blackbird, we'll return the expanded sequence as a simple string
    // We may want to wrap this in a computed attributes object with a special form or something, not sure...
    @ActivityType.EffectModel
    public String run(Mission mission) {
        return mission.sequencing.modelSequence(expand(mission));
    }

    public Sequence expand(Mission mission) {
        var powerOn = new PWR_Turn_On_Heater();
        var powerOff = new PWR_Turn_Off_Heater();
        return new Sequence(
                "", // TODO - decide how to assign seqIds. Here is probably not the right place to do that...
                List.of(
                        TimedCommand.absolute(currentValue(mission.clock), powerOn),
                        TimedCommand.relative(Duration.of(5, MINUTES), powerOff)
                )
        );
    }
}
