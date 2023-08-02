package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.IdentityMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.function.Function;

/**
 * {@link Discrete} monad
 */
public final class DiscreteMonad {
  private DiscreteMonad() {}

  public static <A> Discrete<A> unit(A a) {
    return DiscreteMonadTransformer.unit(IdentityMonad::unit, a);
  }

  public static <A, B> Discrete<B> bind(Discrete<A> a, Function<A, Discrete<B>> f) {
    return DiscreteMonadTransformer.bind(IdentityMonad::bind, a, f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Discrete<B> map(Discrete<A> a, Function<A, B> f) {
    return bind(a, f.andThen(DiscreteMonad::unit));
  }

  public static <A, B> Function<Discrete<A>, Discrete<B>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }

  // Not monadic, strictly speaking, but useful nonetheless.

  public static <A> DynamicsEffect<Discrete<A>> effect(Function<A, A> f) {
    return ExpiringMonad.effect(lift(f));
  }
}
