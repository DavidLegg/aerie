package gov.nasa.jpl.aerie.contrib.streamline.modeling.lenses;

import gov.nasa.jpl.aerie.contrib.streamline.core.*;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import org.apache.commons.math3.analysis.function.Exp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

/**
 * Utilities for representing functions whose range is logically a resource type,
 * where the domain of the function is infeasibly large, but has feasibly large, compact support.
 * <p>
 *     Consider a statement like this, which a modeler might make:
 *     "For every integer n, there is a resource R(n) tracking whether thing n is active or inactive".
 *     Either the number of inputs is infinite, or is so large as to be infeasible to enumerate all possible values.
 *     One case where this comes up in practice is when the set of inputs is not known at modeling time.
 *     Despite being very small in practice, it is infinite in theory because a model written without foreknowledge of
 *     which inputs will actually be used would have to enumerate all inputs that <em>could</em> be used instead.
 * </p>
 * <p>
 *     Such a situation can still be modeled, if we recognize that most of those theorized resources have some "default" value.
 *     We represent all of those by a single default value, and only store the (potentially) non-default values individually.
 * </p>
 * <p>
 *     Putting this more precisely, the idea of <a href="https://en.wikipedia.org/wiki/Support_(mathematics)#Compact_support">compact support</a>
 *     provides a close analogy, where the "default value" plays the role of zero.
 *     It's not exactly true that only functions with compact support are representable, but compact support is often
 *     representable and encompasses the majority of cases we'd like to represent.
 * </p>
 * <p>
 *     Functions in this class are separated primarily by the topology of the domain.
 *     For example, discrete domains which treat every input point as separate and independent are best represented
 *     with a different data structure than continuous domains which talk about ranges of inputs.
 * </p>
 */
public final class CompactSupport {
    private CompactSupport() {}

    /**
     * Use {@link CellRefV2#autoEffects()} with a discrete domain compactly-supported resource function.
     *
     * @see CompactSupport#discreteCompactSupport(Dynamics, EffectTrait)
     */
    public static <T, S extends Dynamics<?, S>> Function<T, MutableResource<S>> discreteCompactSupport(S defaultValue) {
        return discreteCompactSupport(defaultValue, autoEffects());
    }

    /**
     * Returns a resource function that treats the domain as topologically discrete.
     *
     * <p>
     *     The input domain is topologically discrete, meaning any two inputs have completely independent outputs.
     *     All output resources take on {@param defaultValue}, unless changed by an effect.
     *     Since effects are applied to one output resource at a time, only finitely many can be changed,
     *     hence the region of support is always finite.
     *     That is, finitely many resources have non-default values, even if the domain is infinite.
     * </p>
     */
    public static <T, S extends Dynamics<?, S>> Function<T, MutableResource<S>> discreteCompactSupport(S defaultValue, EffectTrait<DynamicsEffect<S>> effectTrait) {
        var defaultDynamics = DynamicsMonad.pure(defaultValue);
        // For efficiency, use a custom cell type whose effects explicitly expose which keys they operate on.
        // This way, we only do auto-effects conflict detection when we actually apply effects to the same virtual resource.
        // Applying effects to different keys of this map is understood to be independent, and therefore allowed, without explicitly testing it.
        var backingCell = CellRefV2.<Discrete<Map<T, ErrorCatching<Expiring<S>>>>, MapEffect<T, S>>allocate(DynamicsMonad.pure(discrete(Map.of())), new EffectTrait<>() {
            @Override
            public MapEffect<T, S> empty() {
                return new MapEffect<>(Map.of(), defaultDynamics);
            }

            @Override
            public MapEffect<T, S> sequentially(MapEffect<T, S> prefix, MapEffect<T, S> suffix) {
                var subEffects = new HashMap<>(prefix.subEffects());
                suffix.subEffects().forEach((k, e) -> subEffects.merge(k, e, effectTrait::sequentially));
                return new MapEffect<>(subEffects, defaultDynamics);
            }

            @Override
            public MapEffect<T, S> concurrently(MapEffect<T, S> left, MapEffect<T, S> right) {
                var subEffects = new HashMap<>(left.subEffects());
                right.subEffects().forEach((k, e) -> subEffects.merge(k, e, effectTrait::concurrently));
                return new MapEffect<>(subEffects, defaultDynamics);
            }
        });

        // Having defined that cell, define a MutableResource lens in terms of a targeted key k:
        return k -> new MutableResource<>() {
            @Override
            public void emit(DynamicsEffect<S> effect) {
                backingCell.emit(new MapEffect<>(Map.of(k, effect), defaultDynamics));
            }

            @Override
            public ErrorCatching<Expiring<S>> getDynamics() {
                // REVIEW: This is the "correct" way to define getDynamics here
                // However, if profiling reveals this to be a performance bottleneck, we could forego the "bind"
                // and just extract the map directly... This would save a few comparisons and indirections
                return DynamicsMonad.bind(backingCell.get().dynamics, d -> d.extract().getOrDefault(k, defaultDynamics));
            }
        };
    }

    private record MapEffect<K, D extends Dynamics<?, D>>(Map<K, DynamicsEffect<D>> subEffects, ErrorCatching<Expiring<D>> defaultDynamics) implements DynamicsEffect<Discrete<Map<K, ErrorCatching<Expiring<D>>>>> {
        @Override
        public ErrorCatching<Expiring<Discrete<Map<K, ErrorCatching<Expiring<D>>>>>> apply(ErrorCatching<Expiring<Discrete<Map<K, ErrorCatching<Expiring<D>>>>>> dynamics) {
            // Apply each sub-effect independently to its corresponding entry.
            return DiscreteDynamicsMonad.<Map<K, ErrorCatching<Expiring<D>>>>effect(m -> {
                var result = new HashMap<>(m);
                for (var entry : subEffects.entrySet()) {
                    result.compute(entry.getKey(), (k, d) -> entry.getValue().apply(d == null ? defaultDynamics : d));
                }
                return result;
            }).apply(dynamics);
        }
    }

    // TODO: Continuous compact support using ranges: "Over the range [a, b], apply this effect..."
}
