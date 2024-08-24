package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentialEquations;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public final class Configuration {
  @Parameter
  public boolean traceResources = false;

  @Parameter
  public boolean profileResources = false;

  @Parameter
  public double approximationTolerance = 1e-2;

  @Parameter
  public Duration profilingDumpTime = Duration.ZERO;

  @Parameter
  public double oscillatorFrequencyHz = 0.1;

  @Parameter
  public int oscillatorTimeStepMillis = 100;

  @Parameter
  public DifferentialModel.ODEIntegrator odeIntegrator = DifferentialModel.ODEIntegrator.RUNGE_KUTTA_4;
}
