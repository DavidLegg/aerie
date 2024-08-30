package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

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
 *     The difference between this and {@link Random}
 *     is that this class uses Aerie cells to track its internal state.
 *     That way, this class meets Aerie's requirements for determinism.
 *     By contrast, {@link Random} would leak state across tasks and generate
 *     different numbers across task replays, violating Aerie's requirements for determinism.
 * </p>
 * <p>
 *     Since accessing this class concurrently from separate tasks would generate (at best) equal results,
 *     thereby introducing correlation between nominally independent tasks,
 *     and at worst would produce an inconsistent seed state,
 *     this class prohibits access from concurrent Aerie tasks.
 *     Attempting to do so will throw an error.
 * </p>
 */
public class AerieRandom extends Random {
    private final MutableResource<Discrete<Long>> seed;

    public AerieRandom(long seed) {
        // Use non-commuting effects to completely prohibit concurrent access.
        this.seed = resource(discrete((seed ^ 0x5DEECE66DL) & ((1L << 48) - 1)), noncommutingEffects());
    }

    // I believe that by overriding next(int), we have fully overridden where this generator keeps its state
    // while getting all the methods on Random for free, without needing to re-write nextInt(), nextDouble(), etc.

    @Override
    public int next(int bits) {
        // Iteration algorithm copied from java.util.Random
        seed.emit("Iterate RNG seed", effect(seed$ -> (seed$ * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)));
        return (int)(currentValue(seed) >>> (48 - bits));
    }
}
