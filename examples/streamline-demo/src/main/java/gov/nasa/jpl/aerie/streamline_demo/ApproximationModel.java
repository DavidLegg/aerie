package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Approximation;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentiableDynamics;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Approximation.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.ErrorEstimates.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.secantApproximation;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;

public class ApproximationModel {
  public CellResource<Polynomial> polynomial = cellResource(polynomial(1));

  public Resource<Linear> hacked = map(polynomial, p$ -> new Linear(p$.getCoefficient(0), p$.getCoefficient(1)));

  public Resource<Linear> uniformApproximation = approximate(
      polynomial,
      secantApproximation(IntervalFunctions.<Polynomial>byUniformSampling(MINUTE)));

  public Resource<Linear> differentiableApproximation = approximate(
      map(polynomial, DifferentiableDynamics::asDifferentiable),
      secantApproximation(byBoundingError(
          1e-4,
          SECOND,
          HOUR.times(24),
          errorByQuadraticApproximation())));

  public Resource<Linear> directApproximation = approximate(
      polynomial,
      secantApproximation(IntervalFunctions.<Polynomial>byBoundingError(
          1e-4,
          SECOND,
          HOUR.times(24),
          errorByOptimization())));

  public Resource<Polynomial> divisor = constant(2);
  public Resource<Unstructured<Double>> polynomialOverTwo = UnstructuredResources.map(polynomial, divisor, (p, q) -> p / q);

  public Resource<Linear> uniformApproximation2 = approximate(
      polynomialOverTwo,
      secantApproximation(IntervalFunctions.<Unstructured<Double>>byUniformSampling(MINUTE)));

  public Resource<Linear> directApproximation2 = approximate(
      polynomialOverTwo,
      secantApproximation(IntervalFunctions.<Unstructured<Double>>byBoundingError(
          1e-4,
          SECOND,
          HOUR.times(24),
          errorByOptimization())));

  public ApproximationModel(final Registrar registrar, final Configuration config) {
    registrar.real("approximation/hacked", hacked);
    registrar.real("approximation/uniform", uniformApproximation);
    registrar.real("approximation/differentiable", differentiableApproximation);
    registrar.real("approximation/direct", directApproximation);
    registrar.real("approximation/uniform2", uniformApproximation2);
    registrar.real("approximation/direct2", directApproximation2);
  }
}
