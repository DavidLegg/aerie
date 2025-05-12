package gov.nasa.jpl.aerie.contrib.streamline.utils;

import java.util.function.Function;

public interface InvertibleFunction<A, B> extends Function<A, B> {
    InvertibleFunction<B, A> inverse();

    static <A, B> InvertibleFunction<A, B> of(Function<A, B> forward, Function<B, A> inverse) {
        return new InvertibleFunction<>() {
            @Override
            public B apply(A a) {
                return forward.apply(a);
            }

            @Override
            public InvertibleFunction<B, A> inverse() {
                return of(inverse, forward);
            }
        };
    }

    static <A> InvertibleFunction<A, A> identity() {
        return of(Function.identity(), Function.identity());
    }

    default <C> InvertibleFunction<C, B> compose(InvertibleFunction<C, A> before) {
        return of(
                ((Function<A, B>)this).compose(before),
                ((Function<A, C>) before.inverse()).compose(this.inverse()));
    }

    default <C> InvertibleFunction<A, C> andThen(InvertibleFunction<B, C> after) {
        return after.compose(this);
    }
}
