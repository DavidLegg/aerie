package gov.nasa.jpl.aerie.command_expansion.model;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$enum;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;

public class PowerModel {
    public final MutableResource<Discrete<HeaterState>> heater = discreteResource(HeaterState.OFF);

    public PowerModel(Registrar registrar) {
        registrar.discrete("power.heater", heater, $enum(HeaterState.class));
    }

    public enum HeaterState {
        OFF,
        STANDBY,
        ON
    }
}
