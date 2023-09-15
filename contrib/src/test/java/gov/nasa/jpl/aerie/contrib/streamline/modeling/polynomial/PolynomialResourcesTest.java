package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clampedIntegrate;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.*;

@Nested
@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class PolynomialResourcesTest {
  public PolynomialResourcesTest(final Registrar registrar) {
    Resources.init();
  }

  private final CellResource<Polynomial> integrandForClampedIntegrate = cellResource(polynomial(1));
  private final CellResource<Polynomial> lowerBound = cellResource(polynomial(0.0));
  private final CellResource<Polynomial> upperBound = cellResource(polynomial(100.0));
  private final Resource<Polynomial> clampedIntegral = clampedIntegrate(integrandForClampedIntegrate, 5.0, lowerBound, upperBound);

  @Test
  void clamped_integral_starts_at_starting_value_and_integrand_rate() {
    Polynomial startingDynamics = clampedIntegral.getDynamics().data();
    assertEquals(polynomial(5.0, 1.0), startingDynamics);
  }

  @Test
  void clamped_integral_with_positive_integrand_grows_to_upper_bound_then_stops() {
    assertEquals(polynomial(5.0, 1.0), clampedIntegral.getDynamics().data());
    delay(95, SECONDS);
    // allow a few ticks for integral machinery to settle
    delay(ZERO);
    delay(ZERO);
    delay(ZERO);
    delay(ZERO);
    delay(ZERO);
    delay(ZERO);
    // assertEquals(polynomial(100.0), clampedIntegral.getDynamics().data());
    delay(20, SECONDS);
    assertEquals(polynomial(100.0), clampedIntegral.getDynamics().data());
  }

  // Cases:
  // DONE:
  //   Starting with in-bounds value (starts with that value + initial rate)
  // TODO:
  //   Starting with impossible bounds (error)
  //   Starting with out-of-bounds value (snap to boundary)
  //   Integration of constant integrand without hitting boundary
  //   Integration of linear integrand without hitting boundary
  //   Hit constant boundary
  //   Hit retreating boundary (increasing upper bound, decreasing lower bound)
  //   Hit encroaching boundary (decreasing upper bound, increasing lower bound)
  //   Hit constant boundary, discretely change integrand to move off boundary
  //   Hit constant boundary, discretely change integrand without moving off boundary
  //   Hit (retreating, encroaching) boundary, discretely change integrand to move off boundary
  //   Hit (retreating, encroaching) boundary, discretely change integrand without moving off boundary
  //   Hit constant boundary, discretely change boundary to move off boundary
  //   Hit constant boundary, discretely change boundary without moving off boundary
  //   Hit (retreating, encroaching) boundary, discretely change boundary to move off boundary
  //   Hit (retreating, encroaching) boundary, discretely change boundary without moving off boundary
  //   Hit boundary that continuously changes to retreating, thereby "freeing" integral
  //   Hit boundary with integrand that continuously changes to retreating, thereby "freeing" integral
}
