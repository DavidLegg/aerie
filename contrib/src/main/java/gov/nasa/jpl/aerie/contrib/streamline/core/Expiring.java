package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;

// REVIEW: using an interface here to support a lazy expiration calculation later.
// However, this does introduce some complexity now. Should this be replaced by the record directly?
public interface Expiring<D> {
  // Note: Data must not depend on when it is called.
  // Lazy implementations should return the value as of when this Expiring was created.
  D data();
  // Note: Expiry must not depend on when it is called.
  // Lazy implementations should return the value as of when this Expiring was created.
  Expiry expiry();

  static <D> Expiring<D> expiring(D data, Expiry expiry) {
    return new Expiring$<>(data, expiry);
  }

  static <D> Expiring<D> neverExpiring(D data) {
    return expiring(data, NEVER);
  }

  static <D> Expiring<D> expiring(D data, Duration expiry) {
    return expiring(data, Expiry.at(expiry));
  }

  record Expiring$<D>(D data, Expiry expiry) implements Expiring<D> {}
}
