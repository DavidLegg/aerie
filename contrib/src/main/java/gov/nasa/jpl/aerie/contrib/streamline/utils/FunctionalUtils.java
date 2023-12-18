package gov.nasa.jpl.aerie.contrib.streamline.utils;

import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility functions for functional style programming.
 *
 * Generated by generate_functional_utils.py on 2023-11-28
 * to support functions of up to 20 arguments.
 */
public final class FunctionalUtils {
  private FunctionalUtils() {}

  public static <A, B, Result> Function<A, Function<B, Result>> curry(BiFunction<A, B, Result> function) {
    return a -> b -> function.apply(a, b);
  }
  
  public static <A, B, C, Result> Function<A, Function<B, Function<C, Result>>> curry(TriFunction<A, B, C, Result> function) {
    return a -> b -> c -> function.apply(a, b, c);
  }
  
  public static <A, B, C, D, Result> Function<A, Function<B, Function<C, Function<D, Result>>>> curry(Function4<A, B, C, D, Result> function) {
    return a -> b -> c -> d -> function.apply(a, b, c, d);
  }
  
  public static <A, B, C, D, E, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Result>>>>> curry(Function5<A, B, C, D, E, Result> function) {
    return a -> b -> c -> d -> e -> function.apply(a, b, c, d, e);
  }
  
  public static <A, B, C, D, E, F, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Result>>>>>> curry(Function6<A, B, C, D, E, F, Result> function) {
    return a -> b -> c -> d -> e -> f -> function.apply(a, b, c, d, e, f);
  }
  
  public static <A, B, C, D, E, F, G, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Result>>>>>>> curry(Function7<A, B, C, D, E, F, G, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> function.apply(a, b, c, d, e, f, g);
  }
  
  public static <A, B, C, D, E, F, G, H, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Result>>>>>>>> curry(Function8<A, B, C, D, E, F, G, H, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> function.apply(a, b, c, d, e, f, g, h);
  }
  
  public static <A, B, C, D, E, F, G, H, I, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Result>>>>>>>>> curry(Function9<A, B, C, D, E, F, G, H, I, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> function.apply(a, b, c, d, e, f, g, h, i);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Result>>>>>>>>>> curry(Function10<A, B, C, D, E, F, G, H, I, J, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> function.apply(a, b, c, d, e, f, g, h, i, j);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Result>>>>>>>>>>> curry(Function11<A, B, C, D, E, F, G, H, I, J, K, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> function.apply(a, b, c, d, e, f, g, h, i, j, k);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Result>>>>>>>>>>>> curry(Function12<A, B, C, D, E, F, G, H, I, J, K, L, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Result>>>>>>>>>>>>> curry(Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Result>>>>>>>>>>>>>> curry(Function14<A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Result>>>>>>>>>>>>>>> curry(Function15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> o -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Result>>>>>>>>>>>>>>>> curry(Function16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> o -> p -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Result>>>>>>>>>>>>>>>>> curry(Function17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> o -> p -> q -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Function<R, Result>>>>>>>>>>>>>>>>>> curry(Function18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> o -> p -> q -> r -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Function<R, Function<S, Result>>>>>>>>>>>>>>>>>>> curry(Function19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> o -> p -> q -> r -> s -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
  }
  
  public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Function<R, Function<S, Function<T, Result>>>>>>>>>>>>>>>>>>>> curry(Function20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> function) {
    return a -> b -> c -> d -> e -> f -> g -> h -> i -> j -> k -> l -> m -> n -> o -> p -> q -> r -> s -> t -> function.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t);
  }
}
