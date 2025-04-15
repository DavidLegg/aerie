package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

public class SegmentMap<D extends Dynamics<?, D>> {
    private final TreeMap<Duration, D> generatedSegments;
    private final Iterator<Entry<Duration, D>> segmentIterator;

    public SegmentMap(Stream<Entry<Duration, D>> segments) {
        this(new TreeMap<>(), segments.iterator());
    }

    private SegmentMap(TreeMap<Duration, D> generatedSegments, Iterator<Entry<Duration, D>> segmentIterator) {
        this.generatedSegments = generatedSegments;
        this.segmentIterator = segmentIterator;
    }

    public D get(Duration key) {
        while (segmentIterator.hasNext() && key.longerThan(generatedSegments.lastKey())) {
            var segment = segmentIterator.next();
            generatedSegments.put(segment.getKey(), segment.getValue());
        }

        var targetSegment = generatedSegments.floorEntry(key);
        if (targetSegment == null) {
            throw new IllegalStateException("No segment available for " + key);
        }

        return targetSegment.getValue().step(key.minus(targetSegment.getKey()));
    }
}
