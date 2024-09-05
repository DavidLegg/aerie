package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredResourceApplicative;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.monads.DiscreteResourceDistributionMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.monads.ProbabilityDistributionMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.monads.UnstructuredResourceDistributionApplicative;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;

public final class UnstructuredResourceDistributions {
    private UnstructuredResourceDistributions() {}

    /**
     * Construct a time-varying probability distribution which is constantly a single value.
     */
    public static <A> Resource<Unstructured<ProbabilityDistribution<A>>> constant(A a) {
        return name(UnstructuredResourceDistributionApplicative.pure(a), "%s", a);
    }

    /**
     * Construct a time-varying probability distribution which takes this resource's value with probability 1.
     */
    public static <A> Resource<Unstructured<ProbabilityDistribution<A>>> certain(Resource<Unstructured<A>> a) {
        return UnstructuredResourceApplicative.map(a, ProbabilityDistributionMonad::pure);
    }

    /**
     * Reinterpret a resource as a time-varying single-point distribution.
     * <p>
     *     Note that sampling this distribution when the resource is in error will throw an error.
     * </p>
     * <p>
     *     Also note that the inverse operation, ProbabilityDistribution -> Unstructured Resource, is not generally allowed.
     *     Since a probability distribution may change without advancing simulation time,
     *     it is not a natural definition of an unstructured resource.
     *     Instead, consider using a task to sample the distribution periodically and update a resource as a result.
     * </p>
     */
    public static <A, D extends Dynamics<A, D>> ProbabilityDistribution<A> asDistribution(Resource<D> a) {
        return () -> currentValue(a);
    }

    /**
     * "Forget" that this is a time-varying distribution.
     * In other words, treat variation in time as part of the randomization instead.
     * Note that sampling this distribution when the resource is in error will throw an error.
     */
    public static <A, D extends Dynamics<ProbabilityDistribution<A>, D>> ProbabilityDistribution<A> flatten(Resource<D> a) {
        return ProbabilityDistributionMonad.join(asDistribution(a));
    }
}
