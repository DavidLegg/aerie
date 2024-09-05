package gov.nasa.jpl.aerie.streamline_demo.event_demo.option_2;

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
    In this version, I model the event explicitly as an activity, but the triggering will be implicit.
    I don't show how one might trigger the event, but anything that can spawn that activity like we've built before
    would be sufficient.
 */

public class ExampleModel {
    public final MutableResource<Discrete<Integer>> powerLevel = discreteResource(10);
    public final MutableResource<Discrete<Integer>> heaterLevel = discreteResource(40);
    public final MutableResource<Discrete<Boolean>> computerOperational = discreteResource(true);

    public final Resource<Discrete<Boolean>> lowPowerFault = map(powerLevel, p -> p < 3);
    public final Resource<Discrete<Boolean>> overheatFault = map(heaterLevel, h -> h > 100);
    public final Resource<Discrete<Boolean>> computerResetFault = not(computerOperational);
    public final Resource<Discrete<Boolean>> anyFault = any(lowPowerFault, overheatFault, computerResetFault);

    public final FaultModel faultModel = new FaultModel(anyFault);
    public final FaultProtectionModel faultProtectionModel = new FaultProtectionModel(this);


    // This is a particularly clean separation, which I kind of like.
    // The idea is that the "regular" model defines what a fault is,
    // the FaultProtectionModel defines how to respond to a fault,
    // and the FaultModel does some additional bookkeeping about faults.

    // You could certainly reorganize these.
    // For example, maybe fault protection should own defining the faults, and/or bookkeeping times in fault.
    // Or, maybe you roll fault protection behavior into the main model, as just a separate method that spawns those daemons.
    // You might also want to break out separate bookkeeping for each kind of fault, perhaps by creating several instances
    // of FaultModel.

    // In short, there are a lot of valid options. I don't think we need to marry ourselves to any one way of doing this.
    public class FaultModel {
        public final Resource<Discrete<Boolean>> hasFault;
        public final MutableResource<VariableClock> timeInCurrentFault = resource(pausedStopwatch());
        public final MutableResource<VariableClock> cumulativeTimeInFault = resource(pausedStopwatch());

        public FaultModel(Resource<Discrete<Boolean>> hasFault) {
            this.hasFault = hasFault;
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

            whenever(model.lowPowerFault, () -> {
                delay(10, SECONDS);
                increment(model.powerLevel);
            });

            whenever(model.overheatFault, () -> {
                delay(1, SECONDS);
                decrement(model.heaterLevel);
            });

            whenever(model.computerResetFault, () -> {
                delay(5, MINUTES);
                turnOn(model.computerOperational);
            });
        }
    }
}
