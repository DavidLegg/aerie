package gov.nasa.jpl.aerie.contrib.streamline;

import gov.nasa.jpl.aerie.contrib.streamline.core.InitialConditionManager;
import gov.nasa.jpl.aerie.contrib.streamline.core.InitialConditionManager.InitialConditions;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.InstantClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.registration.Registrar.ErrorBehavior;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.registration.Registration;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.InstantClockResources.clock;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * Dummy "mission model" for streamline itself, to trip the Merlin annotation processor.
 */
public class StreamlineSystem {
    private static Resource<Clock> CLOCK;
    private static Resource<InstantClock> ABSOLUTE_CLOCK;

    public StreamlineSystem(Registrar registrar) {}

    /**
     * Global initialization method for all streamline singletons.
     * This should be called once at the start of mission model construction.
     * <p>
     *     The following are called by this method, and should <em>not</em> be called separately:
     *     <ul>
     *         <li>{@link Registration#init}</li>
     *         <li>{@link Logging#init}</li>
     *         <li>{@link InitialConditionManager#init}</li>
     *     </ul>
     * </p>
     */
    public static void init(
            Instant planStart,
            Registrar baseRegistrar,
            ErrorBehavior errorBehavior,
            InitialConditions initialConditions,
            Consumer<Map<String, SerializedValue>> finconHandler
    ) {
        Logging.init(baseRegistrar);
        InitialConditionManager.init(initialConditions, finconHandler);
        var registrar = new gov.nasa.jpl.aerie.contrib.streamline.modeling.registration.Registrar(baseRegistrar, planStart, errorBehavior);
        Registration.init(registrar);

        CLOCK = clock(ZERO).name("Global Simulation Clock").build();
        ABSOLUTE_CLOCK = clock(planStart).name("Global Absolute Simulation Clock").build();
    }

    /**
     * Alternate to {@link StreamlineSystem#init}, mainly for unit testing.
     * This provides no-op and default arguments for some of the init parameters to reduce boilerplate.
     */
    public static void testInit(
            Instant planStart,
            Registrar baseRegistrar
    ) {
        init(planStart, baseRegistrar, ErrorBehavior.Throw, InitialConditions.of(Map.of()), $ -> {});
    }

    /**
     * Alternate to {@link StreamlineSystem#init}, mainly for unit testing.
     * This provides no-op and default arguments for some of the init parameters to reduce boilerplate.
     */
    public static void testInit(
            Registrar baseRegistrar
    ) {
        testInit(Instant.EPOCH, baseRegistrar);
    }

    public static Duration currentTime() {
      return Resources.currentValue(CLOCK);
    }

    public static Instant currentInstant() {
      return Resources.currentValue(ABSOLUTE_CLOCK);
    }

    public static Resource<Clock> simulationClock() {
      return CLOCK;
    }

    public static Resource<InstantClock> absoluteClock() {
      return ABSOLUTE_CLOCK;
    }
}
