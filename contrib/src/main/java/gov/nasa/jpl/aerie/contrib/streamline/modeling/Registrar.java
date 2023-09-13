package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;

public class Registrar {
  private final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar;

  public Registrar(final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar) {
    this.baseRegistrar = baseRegistrar;
  }

  public <Value> void discrete(final String name, final Resource<Discrete<Value>> resource, final ValueMapper<Value> mapper) {
    baseRegistrar.discrete(name, () -> currentValue(resource), mapper);
  }

  public void real(final String name, final Resource<Linear> resource) {
    baseRegistrar.real(name, () -> {
      var linearDynamics = resource.getDynamics().data();
      return RealDynamics.linear(linearDynamics.extract(), linearDynamics.rate());
    });
  }
}
