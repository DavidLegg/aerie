package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;

import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock.clock;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class ClockEffects {
  private ClockEffects() {}

  public static void restart(CellResource<Clock> stopwatch) {
    stopwatch.emit("Restart", effect(c -> clock(ZERO)));
  }
}
