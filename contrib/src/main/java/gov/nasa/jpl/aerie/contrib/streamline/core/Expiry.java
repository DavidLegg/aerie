package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;
import java.util.stream.Stream;

public interface Expiry {
  Optional<Duration> value();

  Expiry NEVER = Optional::empty;

  static Expiry at(Duration t) {
    return expiry(Optional.of(t));
  }

  static Expiry expiry(Optional<Duration> value) {
    return () -> value;
  }

  default Expiry or(Expiry other) {
    return expiry(
        Stream.concat(value().stream(), other.value().stream()).reduce(Duration::min));
  }

  default Expiry minus(Duration t) {
    return expiry(value().map(v -> v.minus(t)));
  }

  default boolean isNever() {
    return value().isEmpty();
  }

  default int compareTo(Expiry other) {
    if (this.isNever()) {
      if (other.isNever()) {
        return 0;
      } else {
        return 1;
      }
    } else {
      if (other.isNever()) {
        return -1;
      } else {
        return this.value().get().compareTo(other.value().get());
      }
    }
  }

  default boolean shorterThan(Expiry other) {
    return this.compareTo(other) < 0;
  }

  default boolean noShorterThan(Expiry other) {
    return this.compareTo(other) >= 0;
  }

  default boolean longerThan(Expiry other) {
    return this.compareTo(other) > 0;
  }

  default boolean noLongerThan(Expiry other) {
    return this.compareTo(other) <= 0;
  }
}
