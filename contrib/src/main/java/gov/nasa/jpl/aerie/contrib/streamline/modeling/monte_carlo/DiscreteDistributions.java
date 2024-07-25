package gov.nasa.jpl.aerie.contrib.streamline.modeling.monte_carlo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

public final class DiscreteDistributions {
    private DiscreteDistributions() {}

    /**
     * Returns a uniform discrete probability distribution on the given values.
     */
    public static <T> ProbabilityDistribution<T> uniform(Collection<T> values) {
        // Fix the values in a new collection, since values itself may be mutated later
        var fixedValues = new ArrayList<>(values);
        // Casting to int will truncate the double. That's equivalent to floor on non-negative doubles.
        return rng -> fixedValues.get((int) (rng.nextDouble() * fixedValues.size()));
    }

    /**
     * Returns a weighted distribution on a finite list of values.
     * <p>
     *     Values have probability proportional to their relative weight.
     *     That is, value v_i with weight w_i has probability (w_i / sum_j w_j).
     * </p>
     */
    public static <T> ProbabilityDistribution<T> weighted(List<WeightedEntry<T>> values) {
        var lookup = new TreeMap<Double, T>();
        double cumulativeWeight = 0.0;
        for (var entry : values) {
            lookup.put(cumulativeWeight, entry.value());
            cumulativeWeight += entry.relativeWeight();
        }
        double totalWeight = cumulativeWeight;
        return rng -> lookup.floorEntry(rng.nextDouble() * totalWeight).getValue();
    }

    public record WeightedEntry<T>(T value, double relativeWeight) {}
}
