package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;

import java.util.function.Function;

/**
 * Truncated version of {@link ResourceMonad}, {@link Expiring}&lt;A&gt; -> {@link Resource}&lt;A&gt;
 * This version allows the user to extract the expiry during the derivation, which is rarely useful.
 */
public final class ExpiringResourceMonad {
  private ExpiringResourceMonad() {}

  public static <A> Resource<A> unit(Expiring<A> a) {
    return ResourceMonadTransformer.unit(IdentityMonad::unit, a);
  }

  public static <A, B> Resource<B> bind(Resource<A> a, Function<Expiring<A>, Resource<B>> f) {
    return ResourceMonadTransformer.bind(IdentityMonad::bind, a, f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Resource<B> map(Resource<A> a, Function<Expiring<A>, Expiring<B>> f) {
    return bind(a, f.andThen(ExpiringResourceMonad::unit));
  }

  public static <A, B> Function<Resource<A>, Resource<B>> lift(Function<Expiring<A>, Expiring<B>> f) {
    return a -> map(a, f);
  }
}
