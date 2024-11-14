package gov.nasa.jpl.aerie.contrib.streamline.utils;

import java.util.function.Function;

/**
 * Describes a function with a perfect inverse, aka an isomorphism.
 * <p>
 *     Implementations should guarantee that the double-inverse is extensionally equal to the original.
 *     That is, <code>f.inverse().inverse().apply(a)</code> should equal <code>f.apply(a)</code>
 *     for any {@link InvertibleFunction} f and argument a,
 *     for a definition of "equal" appropriate in context.
 * </p>
 */
public interface InvertibleFunction<A, B> extends Function<A, B> {
    InvertibleFunction<B, A> inverse();

    default <C> InvertibleFunction<C, B> compose(InvertibleFunction<C, A> before) {
        return new InvertibleFunction<>() {
            @Override
            public InvertibleFunction<B, C> inverse() {
                return before.inverse().<B>compose(InvertibleFunction.this.inverse());
            }

            @Override
            public B apply(C c) {
                return InvertibleFunction.this.apply(before.apply(c));
            }
        };
    }

    default <C> InvertibleFunction<A, C> andThen(InvertibleFunction<B, C> after) {
        return after.compose(this);
    }

    static <A, B> InvertibleFunction<A, B> of(Function<A, B> f, Function<B, A> fInverse) {
        return new InvertibleFunction<>() {
            @Override
            public B apply(A a) {
                return f.apply(a);
            }

            @Override
            public InvertibleFunction<B, A> inverse() {
                return InvertibleFunction.of(fInverse, f);
            }
        };
    }
}
