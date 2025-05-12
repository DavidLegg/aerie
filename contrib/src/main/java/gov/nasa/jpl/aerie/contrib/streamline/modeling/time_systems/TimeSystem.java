package gov.nasa.jpl.aerie.contrib.streamline.modeling.time_systems;

import gov.nasa.jpl.aerie.contrib.streamline.utils.InvertibleFunction;

import java.util.function.Function;

public sealed interface TimeSystem<T> {
    <S> Function<T, S> conversionTo(TimeSystem<S> other);

    static <T> TimeSystem<T> root(String name) {
        return new TimeSystemImpl<>(name);
    }

    <S> TimeSystem<S> derive(String name, InvertibleFunction<T, S> derivation);

    final class TimeSystemImpl<T, S> implements TimeSystem<T> {
        private final String name;
        private final TimeSystem<?> root;
        private final int level;
        private final TimeSystem<S> parent;
        private final InvertibleFunction<S, T> derivation;

        private TimeSystemImpl(String name) {
            this.name = name;
            this.root = this;
            this.level = 0;
            this.parent = null;
            this.derivation = null;
        }

        private TimeSystemImpl(String name, TimeSystem<?> root, int level, TimeSystem<S> parent, InvertibleFunction<S, T> derivation) {
            this.name = name;
            this.root = root;
            this.level = level;
            this.parent = parent;
            this.derivation = derivation;
        }

        @Override
        public <R> Function<T, R> conversionTo(TimeSystem<R> other) {
            return conversionTo((TimeSystemImpl<R, ?>) other);
        }

        @Override
        public <R> TimeSystem<R> derive(String name, InvertibleFunction<T, R> derivation) {
            return new TimeSystemImpl<>(name, root, level + 1, this, derivation);
        }

        @SuppressWarnings("unchecked")
        public <R, U> Function<T, R> conversionTo(TimeSystemImpl<R, U> other) {
            // Termination: We will walk one side's ancestors until we are at the same level on both sides,
            // as measured from the tree root. From then on, we will compare every pair of same-level nodes.
            // By necessity, this will include the LCA. By definition, the LCA will be the first to be equal.

            if (this == other) {
                return (Function<T, R>) Function.identity();
            }
            if (this.root != other.root) {
                throw new IllegalArgumentException(
                        "Time systems %s and %s are not compatible.".formatted(this, other));
            }

            // Null safety: parent and derivation are null iff this is a root.
            // This and other share a root, so if this is a root, other is not a root.
            // If other is not a root, other.level > 0, hence we do not access this.parent or this.derivation.
            // By similar logic, we don't access other.parent or other.derivation if other is a root.
            if (this.level >= other.level) {
                return this.parent.conversionTo(other).compose(derivation.inverse());
            } else {
                return other.derivation.compose(this.conversionTo(other.parent));
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
