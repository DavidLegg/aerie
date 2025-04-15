package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * General wrapper for "piecewise" extension of dynamics.
 * <p>
 *     If a regular dynamics object like {@link gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial}
 *     represents a single "segment" of a resource, the Piecewise wrapper extends this to multiple segments in a single
 *     dynamics object.
 * </p>
 * <p>
 *     This can be helpful for reducing the number of intermediate tasks and update cells.
 *     For example, the max of A and B, without Piecewise, requires a monitoring task and a signalling cell to detect
 *     and signal to downstream consumers when the result changes due to continuous evolution of the dynamics
 *     (rather than due directly to effects).
 *     With Piecewise, the entire result can be represented as a single Dynamics object with two sub-segments.
 *     Only if the downstream consumers need to signal this result to Aerie do they need to create a monitoring task
 *     and signalling cell. Otherwise, they need not pay the overhead of running those components.
 * </p>
 */
public class Piecewise<V, D extends Dynamics<V, D>> implements Dynamics<V, Piecewise<V, D>> {
    private final SegmentMap<D> segments;
    private final Duration offset;

    /**
     * Construct a Piecewise dynamics object from a stream of segments.
     * We assume that each segment is applicable until the start of the next,
     * or for the rest of time if there are no further segments.
     */
    public Piecewise(Stream<Entry<Duration, D>> segments) {
        this(new SegmentMap<>(segments), ZERO);
    }

    private Piecewise(SegmentMap<D> segments, Duration offset) {
        this.segments = segments;
        this.offset = offset;
    }

    @Override
    public V extract() {
        return segments.get(offset).extract();
    }

    @Override
    public Piecewise<V, D> step(Duration t) {
        // Importantly, *share* the mutable segments map between Piecewise objects.
        // That way, each object benefits from the caching done by the others.
        return new Piecewise<>(segments, offset.plus(t));
    }
}
