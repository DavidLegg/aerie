package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;

public final class Mission {
  public final DataModel dataModel;

  public Mission(final gov.nasa.jpl.aerie.merlin.framework.Registrar registrar, final Configuration config) {
    dataModel = new DataModel(new Registrar(registrar), config);
  }
}
