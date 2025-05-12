package gov.nasa.jpl.aerie.contrib.streamline.modeling.time_systems;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredResourceApplicative;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.utils.InvertibleFunction;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import spice.basic.SCLK;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources.asUnstructured;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

public class Demo {
    {
        Instant planStart = Instant.now();
        // In practice, OWLT would be pulled from SPICE or something.
        // For simplicity of this example, I'm just directly storing it here.
        Resource<Discrete<Duration>> oneWayLightTime = discreteResource(Duration.HOUR);

        TimeSystem<Duration> AERIE_SIM_TIME = TimeSystem.root("Aerie Simulation Time");
        TimeSystem<Instant> UTC = AERIE_SIM_TIME.derive("UTC", InvertibleFunction.of(
                simTime -> planStart.plusNanos(simTime.in(MICROSECONDS) * 1_000),
                utc -> Duration.of(ChronoUnit.MICROS.between(planStart, utc), MICROSECONDS)
        ));
        TimeSystem<Instant> SCET = UTC.derive("SCET", InvertibleFunction.identity());
        // "Predicted ERT" isn't really a "time system", per se... ERT is a time system, and adding OWLT to SCET predicts an ERT.
        TimeSystem<Instant> predictedERT = SCET.derive("Predicted ERT", InvertibleFunction.of(
                scet -> scet.plusNanos(currentValue(oneWayLightTime).in(MICROSECONDS) * 1_000),
                ert -> ert.minusNanos(currentValue(oneWayLightTime).in(MICROSECONDS) * 1_000)
        ));
        TimeSystem<SCLK> SCLK = SCET.derive("SCLK", InvertibleFunction.of(
                scet -> /* pretend this is a spice call */ null,
                sclk -> /* pretend this is a spice call */ null
        ));

        TimeSystem<String> UTC_DOY = UTC.derive("UTC Day-of-Year format", InvertibleFunction.of(
                utc -> /* format instant to utc DOY string */ "",
                utc_doy -> /* parse utc time from DOY string */ null
        ));
        TimeSystem<String> PST_DOY = UTC.derive("PST Day-of-Year format", InvertibleFunction.of(
                utc -> /* format instant to PST DOY string */ "",
                utc_doy -> /* parse pst time from DOY string */ null
        ));

        // etc. with derived time systems, as needed.


        Time<Duration> simulationTime = new Time<>(Duration.ZERO, AERIE_SIM_TIME);
        Time<Instant> utcTime = simulationTime.convertTo(UTC);
        Time<String> pstTime = utcTime.convertTo(PST_DOY);


        Resource<Clock> SIMULATION_CLOCK = clock();
        // These are technically subverting the resource system mildly, by not being true dynamics objects.
        Resource<Time<Duration>> SIMULATION_CLOCK_WITH_SYSTEM = map(SIMULATION_CLOCK, c -> new Time<>(c.extract(), AERIE_SIM_TIME));
        Resource<Time<Instant>> UTC_CLOCK = map(SIMULATION_CLOCK_WITH_SYSTEM, t -> t.convertTo(UTC));
        Resource<Time<SCLK>> SCLK_CLOCK = map(SIMULATION_CLOCK_WITH_SYSTEM, t -> t.convertTo(SCLK));

        Resource<Unstructured<Time<Duration>>> BASE_CLOCK = UnstructuredResourceApplicative.map(asUnstructured(SIMULATION_CLOCK), t -> new Time<>(t, AERIE_SIM_TIME));
        Resource<Unstructured<Time<Instant>>> UTC_CLOCK_2 = UnstructuredResourceApplicative.map(BASE_CLOCK, t -> t.convertTo(UTC));
        Resource<Unstructured<Instant>> IMPLICIT_UTC_CLOCK = UnstructuredResourceApplicative.map(UTC_CLOCK_2, Time::time);
        Resource<Unstructured<Time<SCLK>>> SCLK_CLOCK_2 = UnstructuredResourceApplicative.map(BASE_CLOCK, t -> t.convertTo(SCLK));

        {
            // Reading a time using approach 1, quasi-dynamics:
            Instant utcInstant = currentData(UTC_CLOCK).time();

            // Reading a time using approach 2, unstructured dynamics:
            Instant utcInstant2 = currentValue(UTC_CLOCK_2).time();
            // or, folding that .time() call into a resource,
            Instant utcInstant3 = currentValue(IMPLICIT_UTC_CLOCK);

            // Or perhaps the best approach, doing the conversion at the point-of-use instead of within the resource:
            Instant utcInstant4 = currentValue(BASE_CLOCK).convertTo(UTC).time();
        }
    }
}
