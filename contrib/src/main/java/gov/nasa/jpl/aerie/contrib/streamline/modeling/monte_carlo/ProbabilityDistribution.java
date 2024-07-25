package gov.nasa.jpl.aerie.contrib.streamline.modeling.monte_carlo;

public interface ProbabilityDistribution<T> {
    T sample(AerieRandom rng);
}
