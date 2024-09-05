package gov.nasa.jpl.aerie.streamline_demo.event_demo.option_3;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock.pausedStopwatch;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClockEffects.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTES;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

/*
    This is the same as option 2, but I've moved the fault definition into the fault activity and the fault protection model.
    This would allow you to have, for example, a "compound fault" activity, which trips multiple kinds of "badness",
    and awaits all of them to be resolved before marking itself fully "handled".
 */

public class ExampleModel {
    public final MutableResource<Discrete<Integer>> powerLevel = discreteResource(10);
    public final MutableResource<Discrete<Integer>> heaterLevel = discreteResource(40);
    public final MutableResource<Discrete<Boolean>> computerOperational = discreteResource(true);

    public final FaultModel faultModel = new FaultModel();
    public final FaultProtectionModel faultProtectionModel = new FaultProtectionModel(this);

    public class FaultModel {
        public final MutableResource<Discrete<Integer>> activeFaults = discreteResource(0);
        public final MutableResource<VariableClock> timeInCurrentFault = resource(pausedStopwatch());
        public final MutableResource<VariableClock> cumulativeTimeInFault = resource(pausedStopwatch());

        public FaultModel() {
            var hasFault = map(activeFaults, $ -> $ > 0);
            whenever(hasFault, () -> {
                start(timeInCurrentFault);
                start(cumulativeTimeInFault);
                waitUntil(when(not(hasFault)));
                reset(timeInCurrentFault);
                pause(cumulativeTimeInFault);
            });
        }
    }

    public class FaultProtectionModel {
        public FaultProtectionModel(ExampleModel model) {
            // Obviously, I have a pretty bare-bones fault response that just corrects the offending resource.
            // In a real model, you'd probably be doing other things that indirectly fix the offending resource,
            // like turning off devices to improve your power situation, or spawning a TurnOnComputer activity.
            // Once again, there's no need to deal with that explicitly here.

            whenever(map(powerLevel, p -> p < 3), () -> {
                delay(10, SECONDS);
                increment(model.powerLevel);
            });

            whenever(map(heaterLevel, h -> h > 100), () -> {
                delay(1, SECONDS);
                decrement(model.heaterLevel);
            });

            whenever(not(computerOperational), () -> {
                delay(5, MINUTES);
                turnOn(model.computerOperational);
            });
        }
    }
}
