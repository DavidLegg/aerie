package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;

public record Expiring<D>(D data, Expiry expiry) {
  public static <D> Expiring<D> expiring(D data, Expiry expiry) {
    return new Expiring<>(data, expiry);
  }

  public static <D> Expiring<D> neverExpiring(D data) {
    return expiring(data, NEVER);
  }

  public static <D> Expiring<D> expiring(D data, Duration expiry) {
    return expiring(data, Expiry.at(expiry));
  }

  public static <D extends Dynamics<?, D>> boolean areEqualResults(Expiring<D> original, Expiring<D> leftResult, Expiring<D> rightResult) {
    return original.data.areEqualResults(leftResult.data, rightResult.data) && leftResult.expiry.equals(rightResult.expiry);
  }
}
