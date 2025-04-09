package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.Demo.OnOff;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

@ActivityType("SwitchCsrDevice")
public class SwitchCsrDevice {
    @Export.Parameter
    public String deviceName;

    @Export.Parameter
    public OnOff state;

    @ActivityType.EffectModel
    public void run(Mission model) {
        DiscreteEffects.set(model.csrModel.isOn(deviceName), state);
    }
}
