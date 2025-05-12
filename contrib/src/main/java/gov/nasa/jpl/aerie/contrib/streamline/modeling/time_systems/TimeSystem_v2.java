package gov.nasa.jpl.aerie.contrib.streamline.modeling.time_systems;

import gov.nasa.jpl.aerie.contrib.streamline.utils.InvertibleFunction;

public sealed interface TimeSystem_v2<T> {
    <S> InvertibleFunction<T, S> conversionTo(TimeSystem_v2<S> other);

    static <T> TimeSystem_v2<T> root() {
    }

    static <T, S> TimeSystem_v2<S> derived(TimeSystem_v2<T> parent, InvertibleFunction<T, S> derivation) {
    }

    final class RootTimeSystem<T> implements TimeSystem_v2<T> {
        private RootTimeSystem() {}

        @Override
        public <S> InvertibleFunction<T, S> conversionTo(TimeSystem_v2<S> other) {
            if (this == other) {
                return (InvertibleFunction<T, S>) InvertibleFunction.identity();
            }
            if (other instanceof TimeSystem_v2.RootTimeSystem<S>) {
                // TODO: add names and debugging info
                throw new IllegalArgumentException();
            }
            return other.conversionTo(this).inverse();
        }
    }

    final class DerivedTimeSystem<T, P> implements TimeSystem_v2<T> {
        final TimeSystem_v2<?> root;
        final TimeSystem_v2<P> parent;
        final InvertibleFunction<P, T> derivation;

        private DerivedTimeSystem(TimeSystem_v2<P> parent, InvertibleFunction<P, T> derivation) {
            this.parent = parent;
            this.derivation = derivation;

            root = parent instanceof TimeSystem_v2.DerivedTimeSystem<?, ?> parentDerived ? parentDerived.root : parent;
        }

        @Override
        public <S> InvertibleFunction<T, S> conversionTo(TimeSystem_v2<S> other) {
            if (this == other) {
                return (InvertibleFunction<T, S>) InvertibleFunction.identity();
            }

            // TODO: use helper method trick here.
            if (other instanceof TimeSystem_v2.DerivedTimeSystem<S,?> derivedOther) {
                for (TimeSystem_v2<?> ancestor = this.parent;
                     ancestor instanceof TimeSystem_v2.DerivedTimeSystem<?,?>;
                     ancestor = ((DerivedTimeSystem<?, ?>) ancestor).parent) {
                    if (ancestor == other) {
                        return parent.conversionTo(other).compose(derivation.inverse());
                    }
                }

                return this.conversionTo(derivedOther.parent).compose(derivedOther.derivation);

            } else {
                // Other is the root system
                if (this.root != other) {
                    // TODO
                    throw new IllegalArgumentException();
                }
            }

            var otherRoot = other instanceof TimeSystem_v2.DerivedTimeSystem<?,?> otherDerived ? otherDerived.root : other;
            if (otherRoot != this.root) {
                // TODO: names and debug info
                throw new IllegalArgumentException();
            }

            // other is not an ancestor of this
            return this.conversionTo(((DerivedTimeSystem<?, ?>) other).parent).compose(other.derivation());
        }
    }
}
