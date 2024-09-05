package gov.nasa.jpl.aerie.streamline_demo.event_demo.option_3;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.not;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * You could certainly model this fault without an activity, as it's just a resource change.
 * This activity provides a natural place to define when the fault is resolved, though.
 */
@ActivityType("ExampleFaultActivity")
public class CauseOverheatFault {
    @Export.Parameter
    public int heaterLevel = 120;

    @ActivityType.EffectModel
    public void run(ExampleModel model) {
        LOGGER.info("Injecting fault");
        set(model.heaterLevel, heaterLevel);
        increment(model.faultModel.activeFaults);

        waitUntil(when(map(model.heaterLevel, h -> h <= 100)));
        decrement(model.faultModel.activeFaults);
        LOGGER.info("Fault resolved");
    }
}
