package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.Domain;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.*;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.Comparison.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.LinearExpression.lx;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.add;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clamp;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.lessThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.max;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.subtract;

public class DataModel {
  public CellResource<Polynomial> desiredRateA = cellResource(polynomial(0));
  public CellResource<Polynomial> desiredRateB = cellResource(polynomial(0));
  public CellResource<Polynomial> desiredRateC = cellResource(polynomial(0));
  public CellResource<Polynomial> upperBoundOnTotalVolume = cellResource(polynomial(10));

  public Resource<Polynomial> actualRateA, actualRateB, actualRateC;
  public Resource<Polynomial> volumeA, volumeB, volumeC, totalVolume;

  public DataModel(final Registrar registrar, final Configuration config) {
    var solver = new LinearArcConsistencySolver("DataModel");
    // Solve for upper bounds and rates, since they're inter-dependent variables
    var volumeA = solver.variable("volumeA", Domain::upperBound);
    var volumeB = solver.variable("volumeB", Domain::upperBound);
    var volumeC = solver.variable("volumeC", Domain::upperBound);
    this.volumeA = volumeA.resource();
    this.volumeB = volumeB.resource();
    this.volumeC = volumeC.resource();
    totalVolume = add(this.volumeA, this.volumeB, this.volumeC);

    var actualRateA = solver.variable("actualRateA", Domain::upperBound);
    var actualRateB = solver.variable("actualRateB", Domain::upperBound);
    var actualRateC = solver.variable("actualRateC", Domain::upperBound);
    this.actualRateA = actualRateA.resource();
    this.actualRateB = actualRateB.resource();
    this.actualRateC = actualRateC.resource();
    // When volume is empty, even if we want to drain volume, actual rate is 0.
    // Otherwise, it's never above desired rate.
    var rateBoundA = trace("rateBoundA", bind(trace("isEmptyA", lessThanOrEquals(trace("volumeA", this.volumeA), 0)), e -> e.extract() ? max(constant(0), desiredRateA) : desiredRateA));
    var rateBoundB = trace("rateBoundB", bind(trace("isEmptyB", lessThanOrEquals(trace("volumeB", this.volumeB), 0)), e -> e.extract() ? max(constant(0), desiredRateB) : desiredRateB));
    var rateBoundC = trace("rateBoundC", bind(trace("isEmptyC", lessThanOrEquals(trace("volumeC", this.volumeC), 0)), e -> e.extract() ? max(constant(0), desiredRateC) : desiredRateC));
    solver.declare(lx(actualRateA), LessThanOrEquals, lx(rateBoundA));
    solver.declare(lx(actualRateB), LessThanOrEquals, lx(rateBoundB));
    solver.declare(lx(actualRateC), LessThanOrEquals, lx(rateBoundC));

    // Clamp the volumes primarily using the solver:
    solver.declare(lx(volumeA).add(lx(volumeB)).add(lx(volumeC)), LessThanOrEquals, lx(upperBoundOnTotalVolume));
    solver.declare(lx(volumeA), GreaterThanOrEquals, lx(0));
    solver.declare(lx(volumeB), GreaterThanOrEquals, lx(0));
    solver.declare(lx(volumeC), GreaterThanOrEquals, lx(0));
    // Do small corrections for overshooting the boundaries using a resource clamp:
    var correctedVolumeA = clamp(this.volumeA, constant(0), upperBoundOnTotalVolume);
    var correctedVolumeB = clamp(this.volumeB, constant(0), subtract(upperBoundOnTotalVolume, correctedVolumeA));
    var correctedVolumeC = clamp(this.volumeC, constant(0), subtract(upperBoundOnTotalVolume, add(correctedVolumeA, correctedVolumeB)));
    // Link rates and corrected volumes:
    solver.declare(lx(volumeA), Equals, lx(actualRateA).integral(correctedVolumeA));
    solver.declare(lx(volumeB), Equals, lx(actualRateB).integral(correctedVolumeB));
    solver.declare(lx(volumeC), Equals, lx(actualRateC).integral(correctedVolumeC));

    registerStates(registrar, config);
  }

  private void registerStates(Registrar registrar, Configuration config) {
    if (config.traceResources) registrar.setTrace();
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
    registrar.clearTrace();
  }

  private static Resource<Linear> linearize(Resource<Polynomial> p) {
    return map(p, p$ -> {
      if (p$.degree() <= 1) {
        return linear(p$.getCoefficient(0), p$.getCoefficient(1));
      } else {
        throw new IllegalStateException("Resource was super-linear");
      }
    });
  }
}
