package gov.nasa.jpl.aerie.streamline_demo.event_demo.option_4;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
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
    Here is again similar to option 2, but in this case, we're asking FaultProtection to define what a fault is entirely.
 */

public class ExampleModel {
    public final MutableResource<Discrete<Integer>> powerLevel = discreteResource(10);
    public final MutableResource<Discrete<Integer>> heaterLevel = discreteResource(40);
    public final MutableResource<Discrete<Boolean>> computerOperational = discreteResource(true);

    public final FaultProtectionModel faultProtectionModel = new FaultProtectionModel(this);

    public class FaultProtectionModel {
        public MutableResource<Discrete<List<Fault>>> activeFaults = discreteResource(List.of());

        public FaultProtectionModel(ExampleModel model) {
            // Obviously, I have a pretty bare-bones fault response that just corrects the offending resource.
            // In a real model, you'd probably be doing other things that indirectly fix the offending resource,
            // like turning off devices to improve your power situation, or spawning a TurnOnComputer activity.
            // Once again, there's no need to deal with that explicitly here.

            var lowPowerLevel = map(powerLevel, p -> p < 3);
            // Detection:
            whenever(lowPowerLevel, () -> {
                var fault = new Fault("Low Power Level", currentTime());
                add(activeFaults, fault);
                waitUntil(when(not(lowPowerLevel)));
                remove(activeFaults, fault);
            });

            // Resolution:
            // Note that resolution may run many times during one fault
            whenever(lowPowerLevel, () -> {
                delay(10, SECONDS);
                increment(model.powerLevel);
            });

            var overheat = map(heaterLevel, h -> h > 100);

            whenever(overheat, () -> {
                var fault = new Fault("Overheat", currentTime());
                add(activeFaults, fault);
                waitUntil(when(not(overheat)));
                remove(activeFaults, fault);
            });
            whenever(overheat, () -> {
                delay(1, SECONDS);
                decrement(model.heaterLevel);
            });

            whenever(not(computerOperational), () -> {
                var fault = new Fault("Computer Not Operational", currentTime());
                add(activeFaults, fault);
                waitUntil(when(computerOperational));
                remove(activeFaults, fault);
            });

            whenever(not(computerOperational), () -> {
                delay(5, MINUTES);
                turnOn(model.computerOperational);
            });
        }

        public record Fault(String name, Duration startTime) {}
    }
}
