package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiry;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise.Lazy.eager;

/*
    Design choices:
    - Do *not* apply "extends Dynamics" restriction to type parameter D on the Profile class.
      Doing so would make Profile<Function<A, B>> illegal, hence prohibit this from being an applicative.
      This would make the Profile type far less transparent to the end user,
      as we wouldn't be able to incorporate it into the resource stack.
    - Implement segments as lazy-computed single-linked lists.
      This is inspired by Haskell's list structure. It allows profiles to contain large or infinitely many segments,
      (consider a periodic function, for example) while maintaining only a few actually computed segments.
      Using a single-linked list instead of a doubly-linked list means we can share tails,
      which should save computation and memory, especially in sharing history.
    - Represent both history and projection (future)
      For marginal additional complexity, representing the full history has potential for wide-ranging improvements.
      For example, integrals could now be a standard derived resource, instead of a monitoring task, though
      we may need to still cache them somehow for performance reasons.
 */

public class Profile<D> {
    private final Segment<D> projection, history;

    private Profile(Segment<D> projection, Segment<D> history) {
        this.projection = projection;
        this.history = history;
    }

    public static <A> Profile<A> constant(A a) {
        var segment = new Segment<>(a, NEVER, null, t -> a);
        return new Profile<>(segment, segment);
    }

    public static <A extends Dynamics<?, A>> Profile<A> simple(A a) {
        return new Profile<>(Segment.infiniteProjection(a), Segment.infiniteHistory(a));
    }

    private boolean isMidSegment() {
        return projection.dynamics == history.dynamics;
    }

    public D extract() {
        return projection.dynamics;
    }

    public Profile<D> step(Duration t) {
        if (t.isZero()) {
            return this;
        } else if (t.isPositive()) {
            Duration substep;
            Segment<D> steppedProjection, newProjection;

            if (Expiry.at(t).shorterThan(projection.expiry)) {
                // Step within the current projection segment
                substep = t;
                steppedProjection = projection.step(substep);
                newProjection = steppedProjection;
            } else {
                // Stepping past the current projection segment
                substep = projection.expiry().value().orElseThrow();
                steppedProjection = projection.step(substep);
                newProjection = projection.next.get();
            }

            // Always start the projection at newProjection and start history at steppedProjection (flipped)
            // If we were mid-segment, drop the first segment of history, resuming at history.next()
            return new Profile<>(
                    newProjection,
                    new Segment<>(
                            steppedProjection.dynamics,
                            isMidSegment() ? history.expiry().plus(substep) : Expiry.at(substep),
                            isMidSegment() ? history.next() : eager(history),
                            s -> steppedProjection.step.apply(s.times(-1))))
                    .step(t.minus(substep));
        } else {
            return flip().step(t.times(-1)).flip();
        }
    }

    private Profile<D> flip() {
        return new Profile<>(history, projection);
    }

    public <E> Profile<E> apply(Profile<Function<D, E>> f) {
        // TODO: Optimize this when isMidSegment()
        return new Profile<>(Segment.apply(projection, f.projection), Segment.apply(history, f.history));
    }

    public static <D> Profile<D> join(Profile<Profile<D>> profile) {
        // TODO: Optimize this when isMidSegment()
        return new Profile<>(
                Segment.join(profile.projection.map($ -> $.projection)),
                Segment.join(profile.history.map($ -> $.history)));
    }

    // Asserts at compile time that this profile is over true dynamics objects.
    // Consequently, switches stepping logic to use the true dynamics step,
    // rather than the induced step if this profile was built with apply.
    public static <D extends Dynamics<?, D>> Profile<D> regularize(Profile<D> profile) {
        return new Profile<>(
                Segment.regularizeProjection(profile.projection),
                Segment.regularizeHistory(profile.history));
    }


    // Segments are a lazily-computed unidirectional linked list of profile segments.
    // The exact interpretation of this data depends on whether this is a projection or history segment.
    private record Segment<D>(D dynamics, Expiry expiry, Lazy<Segment<D>> next, Function<Duration, D> step) {
        public static <D extends Dynamics<?, D>> Segment<D> infiniteProjection(D dynamics) {
            return Segment.projectionOf(dynamics, NEVER, null);
        }

        public static <D extends Dynamics<?, D>> Segment<D> infiniteHistory(D dynamics) {
            return Segment.historyOf(dynamics, NEVER, null);
        }

        public static <D extends Dynamics<?, D>> Segment<D> projectionOf(D dynamics, Expiry expiry, Lazy<Segment<D>> next) {
            return new Segment<>(dynamics, expiry, next, dynamics::step);
        }

        public static <D extends Dynamics<?, D>> Segment<D> historyOf(D dynamics, Expiry expiry, Lazy<Segment<D>> next) {
            return new Segment<>(dynamics, expiry, next, t -> dynamics.step(t.times(-1)));
        }

        public static <D> Segment<D> of(D dynamics, Expiry expiry, Lazy<Segment<D>> next, BiFunction<D, Duration, D> stepDynamics) {
            return new Segment<>(dynamics, expiry, next, t -> stepDynamics.apply(dynamics, t));
        }

        public Segment<D> step(Duration t) {
            return new Segment<>(step.apply(t), expiry, next, s -> step.apply(s.plus(t)));
        }

        public static <A, B> Segment<B> apply(Segment<A> a, Segment<Function<A, B>> f) {
            return new Segment<>(
                    f.dynamics.apply(a.dynamics),
                    a.expiry.or(f.expiry),
                    new Lazy<>(() -> {
                        if (a.expiry.equals(f.expiry)) {
                            return apply(a.next.get(), f.next.get());
                        } else if (a.expiry.shorterThan(f.expiry)) {
                            return apply(a.next.get(), f.step(a.expiry.value().orElseThrow()));
                        } else {
                            return apply(a.step(f.expiry.value().orElseThrow()), f.next.get());
                        }
                    }),
                    // Since we don't know in general whether the result is a true dynamics,
                    // use an induced step which falls back to stepping the arguments.
                    t -> f.step.apply(t).apply(a.step.apply(t)));
        }

        // Assert at compile time that the object in the segment is a true dynamics
        // Having done so, regularize the stepping procedure to be the true dynamics step, not an apply-induced step.
        // I believe this operation only needs to happen if we're caching a derived result.
        // There could be opportunities for performance improvements if we record when segments are already regular,
        // and avoid redefining them here.
        public static <D extends Dynamics<?, D>> Segment<D> regularizeProjection(Segment<D> segment) {
            return Segment.projectionOf(segment.dynamics, segment.expiry, new Lazy<>(() -> regularizeProjection(segment.next.get())));
        }
        public static <D extends Dynamics<?, D>> Segment<D> regularizeHistory(Segment<D> segment) {
            return Segment.historyOf(segment.dynamics, segment.expiry, new Lazy<>(() -> regularizeHistory(segment.next.get())));
        }

        public static <D> Segment<D> join(Segment<Segment<D>> segment) {
            var subsegment = segment.dynamics;
            return new Segment<>(
                    subsegment.dynamics,
                    subsegment.expiry.or(segment.expiry),
                    new Lazy<>(() -> {
                        if (subsegment.expiry.shorterThan(segment.expiry)) {
                            var nextSubsegment = subsegment.next.get();
                            var substep = subsegment.expiry.value().orElseThrow();
                            return join(new Segment<>(
                                    nextSubsegment,
                                    segment.expiry.minus(substep),
                                    segment.next,
                                    t -> segment.step.apply(t.plus(substep))));
                        } else {
                            return join(segment.next.get());
                        }
                    }),
                    subsegment.step);
        }

        public <E> Segment<E> map(Function<D, E> f) {
            return new Segment<>(
                    f.apply(dynamics),
                    expiry,
                    new Lazy<>(() -> next.get().map(f)),
                    f.compose(step));
        }
    }
}
