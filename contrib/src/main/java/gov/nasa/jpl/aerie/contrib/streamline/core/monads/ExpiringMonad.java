package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;

import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;

/**
 * The {@link Expiring} monad, which demands derived values expire no later than their sources.
 */
public final class ExpiringMonad {
  private ExpiringMonad() {}

  public static <A> Expiring<A> unit(A data) {
    return Expiring.neverExpiring(data);
  }

  public static <A, B> Expiring<B> bind(Expiring<A> a, Function<A, Expiring<B>> f) {
    var b = f.apply(a.data());
    return expiring(b.data(), a.expiry().or(b.expiry()));
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Expiring<B> map(Expiring<A> a, Function<A, B> f) {
    return bind(a, f.andThen(ExpiringMonad::unit));
  }

  public static <A, B> Function<Expiring<A>, Expiring<B>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }

  // Not monadic, strictly speaking, but useful nonetheless.

  public static <A extends Dynamics<?, A>> DynamicsEffect<A> effect(Function<A, A> f) {
    return a -> neverExpiring(f.apply(a.data()));
  }
}
