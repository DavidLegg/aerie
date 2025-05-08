package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.streamline.generated.AutoValueMappers;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.InstantClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableInstantClock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.utils.InvertibleFunction;
import gov.nasa.jpl.aerie.contrib.streamline.utils.ValueMapperUtils;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;

import java.time.Instant;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.*;

public final class ValueMappers {
    private ValueMappers() {}

    // General types

    public static ValueMapper<Instant> isoInstant() {
        return ValueMapperUtils.map(string(), InvertibleFunction.of(Instant::parse, Instant::toString));
    }

    // Dynamics

    public static <V> ValueMapper<Discrete<V>> discrete(ValueMapper<V> valueMapper) {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_discrete_Discrete(valueMapper);
    }

    public static ValueMapper<Linear> linear() {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_linear_Linear($double(), $double());
    }

    public static ValueMapper<Polynomial> polynomial() {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_polynomial_Polynomial(doubleArray());
    }

    public static ValueMapper<VariableClock> variableClock() {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_clocks_VariableClock(duration(), $int());
    }

    public static ValueMapper<InstantClock> instantClock() {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_clocks_InstantClock(isoInstant());
    }

    public static ValueMapper<Clock> clock() {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_clocks_Clock(duration());
    }

    public static ValueMapper<VariableInstantClock> variableInstantClock() {
        return AutoValueMappers.gov_nasa_jpl_aerie_contrib_streamline_modeling_clocks_VariableInstantClock(isoInstant(), $int());
    }
}
