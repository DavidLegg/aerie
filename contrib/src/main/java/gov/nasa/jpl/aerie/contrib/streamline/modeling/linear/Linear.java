package gov.nasa.jpl.aerie.contrib.streamline.modeling.linear;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

// TODO: Implement better support for going to/from Linear
// And possibly implement a registrar or at least registrar methods for this.
public interface Linear extends Dynamics<Double, Linear> {
  double rate();

  @Override
  default Linear step(Duration t) {
    return linear(extract() + t.ratioOver(SECOND) * rate(), rate());
  }

  static Linear linear(double value, double rate) {
    return new Linear() {
      @Override
      public double rate() {
        return rate;
      }

      @Override
      public Double extract() {
        return value;
      }
    };
  }
}
