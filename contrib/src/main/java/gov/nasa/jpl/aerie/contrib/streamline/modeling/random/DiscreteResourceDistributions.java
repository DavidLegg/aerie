package gov.nasa.jpl.aerie.contrib.streamline.modeling.random;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.monads.DiscreteResourceDistributionMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.random.monads.ProbabilityDistributionMonad;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;

public final class DiscreteResourceDistributions {
    private DiscreteResourceDistributions() {}

    public static <A> Resource<Discrete<ProbabilityDistribution<A>>> constant(A a) {
        return name(DiscreteResourceDistributionMonad.pure(a), "%s", a);
    }

    public static <A> Resource<Discrete<ProbabilityDistribution<A>>> certain(Resource<Discrete<A>> a) {
        return DiscreteResourceMonad.map(a, ProbabilityDistributionMonad::pure);
    }
}
