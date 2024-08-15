package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.utils.*;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.utils.FunctionalUtils.curry;

/**
 * The applicative (not a monad) formed by composing
 * {@link ResourceMonad} and {@link UnstructuredMonad}
 */
public final class UnstructuredResourceApplicative {
    private UnstructuredResourceApplicative() {}

    public static <A> Resource<Unstructured<A>> pure(A a) {
        return ResourceMonad.pure(UnstructuredMonad.pure(a));
    }

    public static <A, B> Resource<Unstructured<B>> apply(Resource<Unstructured<A>> a, Resource<Unstructured<Function<A, B>>> f) {
        return ResourceMonad.apply(a, ResourceMonad.map(f, UnstructuredMonad::apply));
    }

    // Unstructured<Resource<A>> has a success status and expiry that can vary with time, as the dynamics are stepped forward.
    // Resource<Unstructured<A>> has a single success status and expiry once getDynamics is called.
    // Since the direction required below would lose information, we can't write it in general.
    // This downgrades this structure to an applicative functor, rather than a monad.
    // private static <A> Resource<Unstructured<A>> distribute(Unstructured<Resource<A>> a) {
    // }

    // public static <A> Resource<Unstructured<A>> join(Resource<Unstructured<Resource<Unstructured<A>>>> a) {
    //     return ResourceMonad.map(ResourceMonad.join(ResourceMonad.map(a, UnstructuredResourceMonad::distribute)), UnstructuredMonad::join);
    // }

    /**
     * Efficiently reduce a collection of unstructured resources using an operator on their values.
     *
     * @see UnstructuredResourceApplicative#reduce(Collection, Object, BiFunction, String)
     * @see ResourceMonad#reduce(Collection, Object, BiFunction)
     */
    public static <A> Resource<Unstructured<A>> reduce(Collection<? extends Resource<Unstructured<A>>> operands, A identity, BiFunction<A, A, A> f) {
        return ResourceMonad.reduce(operands, UnstructuredMonad.pure(identity), UnstructuredMonad.map(f));
    }

    /**
     * Like {@link UnstructuredResourceApplicative#reduce(Collection, Object, BiFunction)}, but names the result.
     *
     * @see UnstructuredResourceApplicative#reduce(Collection, Object, BiFunction)
     * @see ResourceMonad#reduce(Collection, Object, BiFunction, String)
     */
    public static <A> Resource<Unstructured<A>> reduce(Collection<? extends Resource<Unstructured<A>>> operands, A identity, BiFunction<A, A, A> f, String operationName) {
        return ResourceMonad.reduce(operands, UnstructuredMonad.pure(identity), UnstructuredMonad.map(f), operationName);
    }

    // GENERATED CODE START
    // Supplemental methods generated by generate_monad_methods.py on 2023-12-06.
    
    public static <A, B> Function<Resource<Unstructured<A>>, Resource<Unstructured<B>>> apply(Resource<Unstructured<Function<A, B>>> f) {
      return a -> apply(a, f);
    }
    
    public static <A, B> Resource<Unstructured<B>> map(Resource<Unstructured<A>> a, Function<A, B> f) {
      return apply(a, pure(f));
    }
    
    public static <A, B> Function<Resource<Unstructured<A>>, Resource<Unstructured<B>>> map(Function<A, B> f) {
      return apply(pure(f));
    }
    
    public static <A, B, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, BiFunction<A, B, Result> function) {
      return map(a, b, curry(function));
    }
    
    public static <A, B, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Function<A, Function<B, Result>> function) {
      return apply(b, map(a, function));
    }
    
    public static <A, B, Result> BiFunction<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<Result>>> map(BiFunction<A, B, Result> function) {
      return (a, b) -> map(a, b, function);
    }
    
    public static <A, B, C, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, TriFunction<A, B, C, Result> function) {
      return map(a, b, c, curry(function));
    }
    
    public static <A, B, C, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Function<A, Function<B, Function<C, Result>>> function) {
      return apply(c, map(a, b, function));
    }
    
    public static <A, B, C, Result> TriFunction<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<Result>>> map(TriFunction<A, B, C, Result> function) {
      return (a, b, c) -> map(a, b, c, function);
    }
    
    public static <A, B, C, D, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Function4<A, B, C, D, Result> function) {
      return map(a, b, c, d, curry(function));
    }
    
    public static <A, B, C, D, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Function<A, Function<B, Function<C, Function<D, Result>>>> function) {
      return apply(d, map(a, b, c, function));
    }
    
    public static <A, B, C, D, Result> Function4<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<Result>>> map(Function4<A, B, C, D, Result> function) {
      return (a, b, c, d) -> map(a, b, c, d, function);
    }
    
    public static <A, B, C, D, E, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Function5<A, B, C, D, E, Result> function) {
      return map(a, b, c, d, e, curry(function));
    }
    
    public static <A, B, C, D, E, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Function<A, Function<B, Function<C, Function<D, Function<E, Result>>>>> function) {
      return apply(e, map(a, b, c, d, function));
    }
    
    public static <A, B, C, D, E, Result> Function5<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<Result>>> map(Function5<A, B, C, D, E, Result> function) {
      return (a, b, c, d, e) -> map(a, b, c, d, e, function);
    }
    
    public static <A, B, C, D, E, F, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Function6<A, B, C, D, E, F, Result> function) {
      return map(a, b, c, d, e, f, curry(function));
    }
    
    public static <A, B, C, D, E, F, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Result>>>>>> function) {
      return apply(f, map(a, b, c, d, e, function));
    }
    
    public static <A, B, C, D, E, F, Result> Function6<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<Result>>> map(Function6<A, B, C, D, E, F, Result> function) {
      return (a, b, c, d, e, f) -> map(a, b, c, d, e, f, function);
    }
    
    public static <A, B, C, D, E, F, G, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Function7<A, B, C, D, E, F, G, Result> function) {
      return map(a, b, c, d, e, f, g, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Result>>>>>>> function) {
      return apply(g, map(a, b, c, d, e, f, function));
    }
    
    public static <A, B, C, D, E, F, G, Result> Function7<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<Result>>> map(Function7<A, B, C, D, E, F, G, Result> function) {
      return (a, b, c, d, e, f, g) -> map(a, b, c, d, e, f, g, function);
    }
    
    public static <A, B, C, D, E, F, G, H, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Function8<A, B, C, D, E, F, G, H, Result> function) {
      return map(a, b, c, d, e, f, g, h, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Result>>>>>>>> function) {
      return apply(h, map(a, b, c, d, e, f, g, function));
    }
    
    public static <A, B, C, D, E, F, G, H, Result> Function8<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<Result>>> map(Function8<A, B, C, D, E, F, G, H, Result> function) {
      return (a, b, c, d, e, f, g, h) -> map(a, b, c, d, e, f, g, h, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Function9<A, B, C, D, E, F, G, H, I, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Result>>>>>>>>> function) {
      return apply(i, map(a, b, c, d, e, f, g, h, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, Result> Function9<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<Result>>> map(Function9<A, B, C, D, E, F, G, H, I, Result> function) {
      return (a, b, c, d, e, f, g, h, i) -> map(a, b, c, d, e, f, g, h, i, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Function10<A, B, C, D, E, F, G, H, I, J, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Result>>>>>>>>>> function) {
      return apply(j, map(a, b, c, d, e, f, g, h, i, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, Result> Function10<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<Result>>> map(Function10<A, B, C, D, E, F, G, H, I, J, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j) -> map(a, b, c, d, e, f, g, h, i, j, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Function11<A, B, C, D, E, F, G, H, I, J, K, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Result>>>>>>>>>>> function) {
      return apply(k, map(a, b, c, d, e, f, g, h, i, j, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, Result> Function11<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<Result>>> map(Function11<A, B, C, D, E, F, G, H, I, J, K, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k) -> map(a, b, c, d, e, f, g, h, i, j, k, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Function12<A, B, C, D, E, F, G, H, I, J, K, L, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Result>>>>>>>>>>>> function) {
      return apply(l, map(a, b, c, d, e, f, g, h, i, j, k, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, Result> Function12<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<Result>>> map(Function12<A, B, C, D, E, F, G, H, I, J, K, L, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l) -> map(a, b, c, d, e, f, g, h, i, j, k, l, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Result>>>>>>>>>>>>> function) {
      return apply(m, map(a, b, c, d, e, f, g, h, i, j, k, l, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, Result> Function13<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<Result>>> map(Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Function14<A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Result>>>>>>>>>>>>>> function) {
      return apply(n, map(a, b, c, d, e, f, g, h, i, j, k, l, m, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> Function14<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<Result>>> map(Function14<A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Function15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Result>>>>>>>>>>>>>>> function) {
      return apply(o, map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> Function15<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<O>>, Resource<Unstructured<Result>>> map(Function15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Function16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Result>>>>>>>>>>>>>>>> function) {
      return apply(p, map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> Function16<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<O>>, Resource<Unstructured<P>>, Resource<Unstructured<Result>>> map(Function16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Function17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Result>>>>>>>>>>>>>>>>> function) {
      return apply(q, map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> Function17<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<O>>, Resource<Unstructured<P>>, Resource<Unstructured<Q>>, Resource<Unstructured<Result>>> map(Function17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Resource<Unstructured<R>> r, Function18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Resource<Unstructured<R>> r, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Function<R, Result>>>>>>>>>>>>>>>>>> function) {
      return apply(r, map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> Function18<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<O>>, Resource<Unstructured<P>>, Resource<Unstructured<Q>>, Resource<Unstructured<R>>, Resource<Unstructured<Result>>> map(Function18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Resource<Unstructured<R>> r, Resource<Unstructured<S>> s, Function19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Resource<Unstructured<R>> r, Resource<Unstructured<S>> s, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Function<R, Function<S, Result>>>>>>>>>>>>>>>>>>> function) {
      return apply(s, map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> Function19<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<O>>, Resource<Unstructured<P>>, Resource<Unstructured<Q>>, Resource<Unstructured<R>>, Resource<Unstructured<S>>, Resource<Unstructured<Result>>> map(Function19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, function);
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Resource<Unstructured<R>> r, Resource<Unstructured<S>> s, Resource<Unstructured<T>> t, Function20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> function) {
      return map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, curry(function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> Resource<Unstructured<Result>> map(Resource<Unstructured<A>> a, Resource<Unstructured<B>> b, Resource<Unstructured<C>> c, Resource<Unstructured<D>> d, Resource<Unstructured<E>> e, Resource<Unstructured<F>> f, Resource<Unstructured<G>> g, Resource<Unstructured<H>> h, Resource<Unstructured<I>> i, Resource<Unstructured<J>> j, Resource<Unstructured<K>> k, Resource<Unstructured<L>> l, Resource<Unstructured<M>> m, Resource<Unstructured<N>> n, Resource<Unstructured<O>> o, Resource<Unstructured<P>> p, Resource<Unstructured<Q>> q, Resource<Unstructured<R>> r, Resource<Unstructured<S>> s, Resource<Unstructured<T>> t, Function<A, Function<B, Function<C, Function<D, Function<E, Function<F, Function<G, Function<H, Function<I, Function<J, Function<K, Function<L, Function<M, Function<N, Function<O, Function<P, Function<Q, Function<R, Function<S, Function<T, Result>>>>>>>>>>>>>>>>>>>> function) {
      return apply(t, map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, function));
    }
    
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> Function20<Resource<Unstructured<A>>, Resource<Unstructured<B>>, Resource<Unstructured<C>>, Resource<Unstructured<D>>, Resource<Unstructured<E>>, Resource<Unstructured<F>>, Resource<Unstructured<G>>, Resource<Unstructured<H>>, Resource<Unstructured<I>>, Resource<Unstructured<J>>, Resource<Unstructured<K>>, Resource<Unstructured<L>>, Resource<Unstructured<M>>, Resource<Unstructured<N>>, Resource<Unstructured<O>>, Resource<Unstructured<P>>, Resource<Unstructured<Q>>, Resource<Unstructured<R>>, Resource<Unstructured<S>>, Resource<Unstructured<T>>, Resource<Unstructured<Result>>> map(Function20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result> function) {
      return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) -> map(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, function);
    }
    // GENERATED CODE END
}
