package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.Domain;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.and;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.not;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.Comparison.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.LinearExpression.lx;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.add;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.greaterThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.integrate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.lessThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.multiply;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.negate;

public class DataModel {
  public CellResource<Polynomial> desiredRateA = cellResource(polynomial(0));
  public CellResource<Polynomial> desiredRateB = cellResource(polynomial(0));
  public CellResource<Polynomial> desiredRateC = cellResource(polynomial(0));
  public CellResource<Polynomial> upperBoundOnTotalVolume = cellResource(polynomial(10));

  public Resource<Polynomial> actualRateA, actualRateB, actualRateC;
  public Resource<Polynomial> volumeA, volumeB, volumeC, totalVolume;

  public DataModel(final Registrar registrar, final Configuration config) {
    var solver = new LinearArcConsistencySolver("gov.nasa.jpl.aerie.streamline_demo.DataModel");
    // Solve for upper bounds and rates, since they're inter-dependent variables

    // XXX: Simplifying assumption: Upper bound is a constant
//    var upperBoundOnA = solver.variable("upperBoundOnA", Domain::upperBound);
//    var upperBoundOnB = solver.variable("upperBoundOnB", Domain::upperBound);
//    var upperBoundOnC = solver.variable("upperBoundOnC", Domain::upperBound);
//    this.upperBoundOnA = upperBoundOnA.resource();
//    this.upperBoundOnB = upperBoundOnB.resource();
//    this.upperBoundOnC = upperBoundOnC.resource();
//    // Total of upperBounds must not exceed the total capacity
//    solver.declare(lx(upperBoundOnA).add(lx(upperBoundOnB)).add(lx(upperBoundOnC)), LessThanOrEqual, lx(upperBoundOnTotalVolume));

    var actualRateA = solver.variable("actualRateA", Domain::upperBound);
    var actualRateB = solver.variable("actualRateB", Domain::upperBound);
    var actualRateC = solver.variable("actualRateC", Domain::upperBound);
    this.actualRateA = actualRateA.resource();
    this.actualRateB = actualRateB.resource();
    this.actualRateC = actualRateC.resource();
    solver.declare(lx(actualRateA), LessThanOrEqual, lx(desiredRateA));
    solver.declare(lx(actualRateB), LessThanOrEqual, lx(desiredRateB));
    solver.declare(lx(actualRateC), LessThanOrEqual, lx(desiredRateC));

    // XXX: Simplifying assumption: all changes are continuous rate changes, not discrete jumps in the integral values
    volumeA = integrate(this.actualRateA, 0);
    volumeB = integrate(this.actualRateB, 0);
    volumeC = integrate(this.actualRateC, 0);

    var aIsEmpty = lessThanOrEquals(volumeA, 0);
    var bIsEmpty = lessThanOrEquals(volumeB, 0);
    var cIsEmpty = lessThanOrEquals(volumeC, 0);

    // Non-negativity
    var aRateLowerBound = ResourceMonad.bind(aIsEmpty, $ -> $.extract() ? constant(0) : constant(Double.NEGATIVE_INFINITY));
    var bRateLowerBound = ResourceMonad.bind(bIsEmpty, $ -> $.extract() ? constant(0) : constant(Double.NEGATIVE_INFINITY));
    var cRateLowerBound = ResourceMonad.bind(cIsEmpty, $ -> $.extract() ? constant(0) : constant(Double.NEGATIVE_INFINITY));
    solver.declare(lx(actualRateA), GreaterThanOrEqual, lx(aRateLowerBound));
    solver.declare(lx(actualRateB), GreaterThanOrEqual, lx(bRateLowerBound));
    solver.declare(lx(actualRateC), GreaterThanOrEqual, lx(cRateLowerBound));

    // "Stealing" conditions
    totalVolume = add(volumeA, volumeB, volumeC);
    var isFull = greaterThanOrEquals(totalVolume, upperBoundOnTotalVolume);
    var cRateUpperBoundDueToStealing = ResourceMonad.bind(
        and(isFull, not(cIsEmpty)), $ -> $.extract()
            ? negate(add(desiredRateA, desiredRateB))
            : constant(Double.POSITIVE_INFINITY));
    var bRateUpperBoundDueToStealing = ResourceMonad.bind(
        and(isFull, not(bIsEmpty), cIsEmpty), $ -> $.extract()
            ? negate(desiredRateA)
            : constant(Double.POSITIVE_INFINITY));
    solver.declare(lx(actualRateB), LessThanOrEqual, lx(bRateUpperBoundDueToStealing));
    solver.declare(lx(actualRateC), LessThanOrEqual, lx(cRateUpperBoundDueToStealing));

    registerStates(registrar);
  }

  private void registerStates(Registrar registrar) {
    registrar.real("desiredRateA", linearize(desiredRateA));
    registrar.real("desiredRateB", linearize(desiredRateB));
    registrar.real("desiredRateC", linearize(desiredRateC));

    registrar.real("actualRateA", linearize(actualRateA));
    registrar.real("actualRateB", linearize(actualRateB));
    registrar.real("actualRateC", linearize(actualRateC));

    registrar.real("volumeA", linearize(volumeA));
    registrar.real("volumeB", linearize(volumeB));
    registrar.real("volumeC", linearize(volumeC));
    registrar.real("totalVolume", linearize(totalVolume));
    registrar.real("maxVolume", linearize(upperBoundOnTotalVolume));
  }

  private static Resource<Linear> linearize(Resource<Polynomial> p) {
    return ResourceMonad.map(p, p$ -> {
      if (p$.degree() <= 1) {
        return linear(p$.getCoefficient(0), p$.getCoefficient(1));
      } else {
        throw new IllegalStateException("Resource was super-linear");
      }
    });
  }
}
