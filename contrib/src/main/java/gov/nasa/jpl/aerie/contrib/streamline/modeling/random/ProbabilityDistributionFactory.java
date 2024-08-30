package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * Helper for constructing {@link ProbabilityDistribution}s.
 * All constructed instances will use a separate {@link AerieRandom} instance,
 * so they may safely be accessed concurrently.
 * <p>
 *     Note that because this creates {@link AerieRandom}s when it creates a distribution,
 *     all distributions must be created during initialization, not during simulation.
 * </p>
 */
public final class ProbabilityDistributionFactory {
    // This parent RNG *must* be split whenever we create a new distribution.
    // That way, each distribution we create is independent.
    private final AerieRandom rng;

    public ProbabilityDistributionFactory(AerieRandom rng) {
        this.rng = rng;
    }

    /**
     * Split another factory off of this one.
     * The split factory and this factory will have deterministically related, but different, RNGs.
     * This can be helpful for structuring the randomness of a model's initializer.
     * <p>
     *     For example, if a model initializes a tree of sub-models,
     *     splitting the factory along each branch of the tree ensures the seeding of each branch is independent.
     *     That is, adding a new distribution in branch 1 will not change how branch 2 is seeded
     *     if branch 1 and branch 2 have different factories, both split from a single parent factory.
     * </p>
     */
    public ProbabilityDistributionFactory split() {
        return new ProbabilityDistributionFactory(rng.split());
    }

    public <T> ProbabilityDistribution<T> uniform(Collection<T> values) {
        // Fix the values in case the collection we're given ever changes.
        var fixedValues = new ArrayList<>(values);
        var myRng = rng.split();
        return () -> fixedValues.get(myRng.nextInt(fixedValues.size()));
    }

    public <T> ProbabilityDistribution<T> weighted(Collection<Pair<T, Double>> weightedValues) {
        var valuesByCutoff = new TreeMap<Double, T>();
        double cumulativeWeight = 0;
        for (var pair : weightedValues) {
            valuesByCutoff.put(cumulativeWeight, pair.getLeft());
            cumulativeWeight += pair.getRight();
        }
        return uniform(0, cumulativeWeight).map(x -> valuesByCutoff.floorEntry(x).getValue());
    }

    public ProbabilityDistribution<Boolean> bernoulli(double p) {
        return weighted(List.of(Pair.of(true, p), Pair.of(false, 1-p)));
    }

    public ProbabilityDistribution<Double> uniform(double min, double max) {
        var myRng = rng.split();
        return () -> myRng.nextDouble(min, max);
    }

    public ProbabilityDistribution<Double> gaussian(double mean, double stddev) {
        var myRng = rng.split();
        return () -> myRng.nextGaussian(mean, stddev);
    }
}
