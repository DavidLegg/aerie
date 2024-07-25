package gov.nasa.jpl.aerie.contrib.streamline.modeling.monte_carlo;

public final class UniformContinuousDistributions {
    private UniformContinuousDistributions() {}

    /**
     * Uniform distribution on [0, 1]
     */
    public static ContinuousDistribution unit() {
        return AerieRandom::nextDouble;
    }

    /**
     * Uniform distribution on [low, high]
     */
    public static ContinuousDistribution uniformBetween(double low, double high) {
        return rng -> {
            double t = rng.nextDouble();
            return t * high + (1 - t) * low;
        };
    }
}
