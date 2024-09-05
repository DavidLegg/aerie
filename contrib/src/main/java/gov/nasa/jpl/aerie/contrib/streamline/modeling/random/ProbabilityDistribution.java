package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

/**
 * A probability distribution over values of type T.
 * <p>
 *     Most probability distributions should either hold an instance of {@link AerieRandom},
 *     or else be derived from a distribution that does.
 *     Use of {@link java.util.Random} should be avoided, as it may leak state across tasks and times
 *     inconsistent with Aerie's requirements.
 * </p>
 */
public interface ProbabilityDistribution<T> {
    T sample();
}
