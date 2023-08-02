package gov.nasa.jpl.aerie.contrib.streamline.modeling.logs;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteMonad.effect;

public final class LogEffects {
  private LogEffects() {}

  public static void log(CellResource<Discrete<Log>> resource, String message) {
    resource.emit(effect(log -> log.append(message)));
  }

  public static void check(boolean condition, CellResource<Discrete<Log>> errorLog, String errorMessage) {
    if (!condition) {
      log(errorLog, errorMessage);
    }
  }
}
