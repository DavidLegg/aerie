package gov.nasa.jpl.aerie.contrib.streamline.modeling.monte_carlo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.Random;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.noncommutingEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;

/**
 * An Aerie-compatible random number generator.
 * <p>
 *     The difference between this and {@link java.util.Random}
 *     is that this class uses Aerie cells to track its internal state.
 *     That way, this class meets Aerie's requirements for determinism.
 *     By contrast, {@link java.util.Random} would leak state across tasks and generate
 *     different numbers across task replays, violating Aerie's requirements for determinism.
 * </p>
 * <p>
 *     Since accessing this class concurrently from separate tasks would generate (at best) equal results,
 *     thereby introducing correlation between nominally independent tasks,
 *     and at worst would produce an inconsistent seed state,
 *     this class prohibits access from concurrent Aerie tasks.
 *     Attempting to do so will throw an error.
 * </p>
 * <p>
 *     Instead, when spawning a task which needs an RNG, use {@link AerieRandom#split} to (deterministically)
 *     generate a new RNG seeded from the value of this RNG, meeting Aerie's requirements for determinism.
 * </p>
 */
public class AerieRandom {
    private final MutableResource<Discrete<Long>> seed;

    public AerieRandom(long seed) {
        // Use non-commuting effects to completely prohibit concurrent access.
        this.seed = resource(discrete((seed ^ 0x5DEECE66DL) & ((1L << 48) - 1)), noncommutingEffects());
    }

    private int next(int bits) {
        seed.emit("Iterate RNG seed", effect(seed$ -> (seed$ * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)));
        return (int)(currentValue(seed) >>> (48 - bits));
    }

    /**
     * Return a uniformly-distributed double between 0.0 and 1.0.
     * @see Random#nextDouble()
     */
    public double nextDouble() {
        return (((long)next(26) << 27) + next(27)) / (double)(1L << 53);
    }

    /**
     * Return a uniformly-distributed long value.
     * @see Random#nextLong()
     */
    public long nextLong() {
        return ((long)next(32) << 32) + next(32);
    }

    /**
     * Generate a new {@link AerieRandom} generator, seeded deterministically from this RNG.
     * The returned generator is independent of this generator, and may be accessed concurrently with this generator.
     */
    public AerieRandom split() {
        return new AerieRandom(nextLong());
    }
}
