package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.registration.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$boolean;
import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$int;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.*;

public class ErrorTestingModel {
  public MutableResource<Discrete<Boolean>> bool = discreteResource(true).name("errorTesting/bool").build();
  public MutableResource<Discrete<Integer>> counter = discreteResource(5).name("errorTesting/counter").build();
  public MutableResource<Polynomial> continuous = polynomialResource(1).name("errorTesting/continuous").build();
  public Resource<Polynomial> derived = multiply(
      continuous,
      asPolynomial(map(counter, c -> (double) c)),
      asPolynomial(map(bool, $ -> $ ? 1.0 : -1.0)));

  public MutableResource<Polynomial> upperBound = polynomialResource(5).name("errorTesting/upperBound").build();
  public MutableResource<Polynomial> lowerBound = polynomialResource(-5).name("errorTesting/lowerBound").build();
  public Resource<Polynomial> clamped = name(clamp(constant(10), lowerBound, upperBound), "errorTesting/clamped");

  public ErrorTestingModel(final Registrar registrar, final Configuration config) {
    registrar.discrete(bool, $boolean());
    registrar.discrete(counter, $int());
    registrar.real(assumeLinear(continuous));
    registrar.real(assumeLinear(derived));
    registrar.real(assumeLinear(lowerBound));
    registrar.real(assumeLinear(upperBound));
    registrar.real(assumeLinear(clamped));
  }
}
