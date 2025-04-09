package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Demo.OnOff;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$enum;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.lenses.CompactSupport.discreteCompactSupport;

public class CsrModel {
    private final Function<String, MutableResource<Discrete<OnOff>>> onPredicate = discreteCompactSupport(discrete(OnOff.OFF));

    public CsrModel(final Registrar registrar, final Configuration config) {
        registrar.discrete("csr.heater", onPredicate.apply("heater"), $enum(OnOff.class));
        registrar.discrete("csr.camera", onPredicate.apply("camera"), $enum(OnOff.class));
        registrar.discrete("csr.starTracker", onPredicate.apply("starTracker"), $enum(OnOff.class));
    }

    public MutableResource<Discrete<OnOff>> isOn(String name) {
        return onPredicate.apply(name);
    }
}
