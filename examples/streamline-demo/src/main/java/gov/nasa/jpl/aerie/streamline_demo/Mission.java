package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.StreamlineSystem;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.registration.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.registration.Registration;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;

import java.time.Instant;

import static gov.nasa.jpl.aerie.streamline_demo.Incons.readIncons;
import static gov.nasa.jpl.aerie.streamline_demo.Incons.writeFincons;

public final class Mission {
  public final DataModel dataModel;
  public final ErrorTestingModel errorTestingModel;
  public final ApproximationModel approximationModel;

  public Mission(final gov.nasa.jpl.aerie.merlin.framework.Registrar registrar$, Instant planStart, final Configuration config) {
    StreamlineSystem.init(
          planStart,
          registrar$,
          Registrar.ErrorBehavior.Log,
          readIncons(config.inconFormat.formatted(planStart)),
          $ -> writeFincons(config.finconFormat.formatted(StreamlineSystem.currentInstant()), $));
    var registrar = Registration.registrar();
    if (config.traceResources) registrar.setTrace();
    if (config.profileResources) Resource.profileAllResources();
    dataModel = new DataModel(registrar, config);
    errorTestingModel = new ErrorTestingModel(registrar, config);
    approximationModel = new ApproximationModel(registrar, config);
    if (config.profilingDumpTime.isPositive()) {
      ModelActions.defer(config.profilingDumpTime, Profiling::dump);
    }
  }
}
