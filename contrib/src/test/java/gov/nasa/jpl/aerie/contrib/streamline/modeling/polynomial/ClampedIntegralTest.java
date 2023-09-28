package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
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
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clampedIntegrate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTE;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class ClampedIntegralTest {

  public ClampedIntegralTest(final Registrar registrar) {
    Resources.init();
  }

  private final CellResource<Polynomial> integrandForClampedIntegrate = cellResource(polynomial(1));
  private final CellResource<Polynomial> lowerBound = cellResource(polynomial(0));
  private final CellResource<Polynomial> upperBound = cellResource(polynomial(100));
  private final Resource<Polynomial> clampedIntegral = clampedIntegrate(integrandForClampedIntegrate, 5, lowerBound, upperBound);

  @Test
  void start_at_starting_value_and_integrand_rate() {
    Polynomial startingDynamics = currentData(clampedIntegral);
    assertEquals(polynomial(5, 1), startingDynamics);
  }

  private final Resource<Polynomial> impossibleStartingIntegral = PolynomialResources.clampedIntegrate(integrandForClampedIntegrate, 5, lowerBound, constant(-1));

  @Test
  void impossible_starting_bounds_throws_an_exception() {
    assertInstanceOf(ErrorCatching.Failure.class, impossibleStartingIntegral.getDynamics());
  }

  @Test
  void discretely_setting_impossible_bounds_throws_an_exception() {
    delay(MINUTE);
    set(lowerBound, polynomial(110));
    settle();
    assertInstanceOf(ErrorCatching.Failure.class, clampedIntegral.getDynamics());
  }

  @Test
  void continuously_changing_to_impossible_bounds_throws_an_exception() {
    set(upperBound, polynomial(10, -1));
    // Delay until the boundaries cross
    delay(duration(10, SECONDS).plus(EPSILON));
    // And allow time for the reacting task to update the integral
    // TODO: Make this more immediate?
    settle();
    assertInstanceOf(ErrorCatching.Failure.class, clampedIntegral.getDynamics());
  }

  private final Resource<Polynomial> clampedIntegralWithTooHighStart = clampedIntegrate(integrandForClampedIntegrate, 110, lowerBound, upperBound);

    @Test
  void starting_value_above_maximum_snaps_to_upper_bound() {
    assertEquals(polynomial(100), currentData(clampedIntegralWithTooHighStart));
  }

  private final Resource<Polynomial> clampedIntegralWithTooLowStart = clampedIntegrate(integrandForClampedIntegrate, -10, lowerBound, upperBound);

    @Test
  void starting_value_below_minimum_snaps_to_lower_bound() {
    assertEquals(polynomial(0, 1), currentData(clampedIntegralWithTooLowStart));
  }

  @Test
  void constant_integrand_behaves_like_regular_integral_within_bounds() {
    delay(10, SECONDS);
    assertEquals(polynomial(15, 1), currentData(clampedIntegral));
  }

  @Test
  void linear_integrand_behaves_like_regular_integral_within_bounds() {
    set(integrandForClampedIntegrate, polynomial(0, 2));
    delay(4, SECONDS);
    // dynamics for integral = (integral 2x) + 5 = x^2 + 5, stepped by 4 (replacing x with x + 4),
    //   = (x + 4)^2 + 5 = x^2 + 8x + 21
    assertEquals(polynomial(21, 8, 1), currentData(clampedIntegral));
  }

  @Test
  void increases_to_upper_bound_then_stops() {
    delay(95, SECONDS);
    assertEquals(100, currentValue(clampedIntegral));
    settle();
    assertEquals(polynomial(100), currentData(clampedIntegral));
    delay(20, SECONDS);
    assertEquals(polynomial(100), currentData(clampedIntegral));
  }

  @Test
  void decreases_to_lower_bound_then_stops() {
    set(integrandForClampedIntegrate, polynomial(-1));
    delay(5, SECONDS);
    assertEquals(0, currentValue(clampedIntegral));
    settle();
    assertEquals(polynomial(0), currentData(clampedIntegral));
    delay(20, SECONDS);
    assertEquals(polynomial(0), currentData(clampedIntegral));
  }

  @Test
  void upper_bound_discretely_crosses_integral() {
    delay(5, SECONDS);
    assertEquals(polynomial(10, 1), currentData(clampedIntegral));
    set(upperBound, polynomial(5));
    settle();
    assertEquals(polynomial(5), currentData(clampedIntegral));
  }

  @Test
  void lower_bound_discretely_crosses_integral() {
    set(integrandForClampedIntegrate, polynomial(-1));
    delay(1, SECONDS);
    assertEquals(polynomial(4, -1), currentData(clampedIntegral));
    set(lowerBound, polynomial(10));
    settle();
    assertEquals(polynomial(10), currentData(clampedIntegral));
  }

  @Test
  void constant_bounds_clamp_identically() {
    set(lowerBound, polynomial(10));
    set(upperBound, polynomial(10));
    delay(1, SECONDS);
    assertEquals(polynomial(10), currentData(clampedIntegral));
    delay(9, SECONDS);
    assertEquals(polynomial(10), currentData(clampedIntegral));
  }

  @Test
  void non_constant_bounds_clamp_identically() {
    set(lowerBound, polynomial(10, -1, 2));
    set(upperBound, polynomial(10, -1, 2));
    delay(1, SECONDS);
    // New dynamics = 2x^2 - x + 10 stepped by 1 seconds:
    //   = 2(x + 1)^2 - (x + 1) + 10 = 2x^2 + 4x + 2 - x - 1 + 10 = 2x^2 + 3x + 11
    assertEquals(polynomial(11, 3, 2), currentData(clampedIntegral));
    delay(9, SECONDS);
    // New dynamics = 2x^2 - x + 10 stepped by 10 seconds:
    //   = 2(x + 10)^2 - (x + 10) + 10 = 2x^2 + 40x + 200 - x - 10 + 10 = 2x^2 + 39x + 200
    assertEquals(polynomial(200, 39, 2), currentData(clampedIntegral));
  }

  @Test
  void increases_to_retreating_upper_bound_then_matches_bound() {
    set(upperBound, polynomial(20, 1));
    set(integrandForClampedIntegrate, polynomial(2));
    settle();
    assertEquals(polynomial(5, 2), currentData(clampedIntegral));
    delay(15, SECONDS);
    assertEquals(35, currentValue(clampedIntegral));
    assertEquals(polynomial(2), currentData(integrandForClampedIntegrate));
    settle();
    assertEquals(polynomial(35, 1), currentData(clampedIntegral));
    assertEquals(polynomial(2), currentData(integrandForClampedIntegrate));
    delay(10, SECONDS);
    assertEquals(polynomial(45, 1), currentData(clampedIntegral));
    assertEquals(polynomial(2), currentData(integrandForClampedIntegrate));
  }

  @Test
  void decreases_to_retreating_upper_bound_then_matches_bound() {
    set(lowerBound, polynomial(0, -1));
    set(integrandForClampedIntegrate, polynomial(-2));
    settle();
    assertEquals(polynomial(5, -2), currentData(clampedIntegral));
    delay(5, SECONDS);
    assertEquals(-5, currentValue(clampedIntegral));
    assertEquals(polynomial(-2), currentData(integrandForClampedIntegrate));
    settle();
    assertEquals(polynomial(-5, -1), currentData(clampedIntegral));
    assertEquals(polynomial(-2), currentData(integrandForClampedIntegrate));
    delay(10, SECONDS);
    assertEquals(polynomial(-15, -1), currentData(clampedIntegral));
    assertEquals(polynomial(-2), currentData(integrandForClampedIntegrate));
  }

  @Test
  void hits_encroaching_upper_bound_then_matches_bound() {
    set(upperBound, polynomial(10, -1));
    set(integrandForClampedIntegrate, polynomial(0));
    delay(5, SECONDS);
    assertEquals(5, currentValue(clampedIntegral));
    settle();
    assertEquals(polynomial(5, -1), currentData(clampedIntegral));
    delay(2, SECONDS);
    assertEquals(polynomial(3, -1), currentData(clampedIntegral));
  }

  @Test
  void hits_encroaching_lower_bound_then_matches_bound() {
    set(lowerBound, polynomial(0, 1));
    set(integrandForClampedIntegrate, polynomial(0));
    delay(5, SECONDS);
    assertEquals(5, currentValue(clampedIntegral));
    settle();
    assertEquals(polynomial(5, 1), currentData(clampedIntegral));
    delay(2, SECONDS);
    assertEquals(polynomial(7, 1), currentData(clampedIntegral));
  }

  @Test
  void hits_upper_bound_then_discretely_change_integrand_sign_to_resume_integration() {
    delay(100, SECONDS);
    assertEquals(polynomial(100), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(-1));
    settle();
    assertEquals(polynomial(100, -1), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(90, -1), currentData(clampedIntegral));
  }

  @Test
  void hits_lower_bound_then_discretely_change_integrand_sign_to_resume_integration() {
    set(integrandForClampedIntegrate, polynomial(-1));
    delay(10, SECONDS);
    assertEquals(polynomial(0), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(1));
    settle();
    assertEquals(polynomial(0, 1), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(10, 1), currentData(clampedIntegral));
  }

  @Test
  void hits_upper_bound_then_discretely_change_integrand_without_resuming_integration() {
    delay(100, SECONDS);
    assertEquals(polynomial(100), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(0.1));
    settle();
    assertEquals(polynomial(100), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(100), currentData(clampedIntegral));
  }

  @Test
  void hits_lower_bound_then_discretely_change_integrand_without_resuming_integration() {
    set(integrandForClampedIntegrate, polynomial(-1));
    delay(10, SECONDS);
    assertEquals(polynomial(0), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(-0.1));
    settle();
    assertEquals(polynomial(0), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(0), currentData(clampedIntegral));
  }

  @Test
  void hits_retreating_upper_bound_then_discretely_change_integrand_to_resume_integration() {
    set(upperBound, polynomial(10, 2));
    set(integrandForClampedIntegrate, polynomial(10));
    delay(10, SECONDS);
    assertEquals(polynomial(30, 2), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(1));
    settle();
    assertEquals(polynomial(30, 1), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(40, 1), currentData(clampedIntegral));
  }

  @Test
  void hits_retreating_upper_bound_then_discretely_change_integrand_without_resuming_integration() {
    set(upperBound, polynomial(10, 2));
    set(integrandForClampedIntegrate, polynomial(10));
    delay(10, SECONDS);
    assertEquals(polynomial(30, 2), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(3));
    settle();
    assertEquals(polynomial(30, 2), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(50, 2), currentData(clampedIntegral));
  }

  @Test
  void hits_retreating_lower_bound_then_discretely_change_integrand_without_resuming_integration() {
    set(lowerBound, polynomial(0, -2));
    set(integrandForClampedIntegrate, polynomial(-10));
    delay(10, SECONDS);
    assertEquals(polynomial(-20, -2), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(-3));
    settle();
    assertEquals(polynomial(-20, -2), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(-40, -2), currentData(clampedIntegral));
  }

  @Test
  void hits_encroaching_upper_bound_then_discretely_change_integrand_without_resuming_integration() {
    set(upperBound, polynomial(20, -1));
    set(integrandForClampedIntegrate, polynomial(10));
    delay(5, SECONDS);
    assertEquals(polynomial(15, -1), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(-0.5));
    settle();
    assertEquals(polynomial(15, -1), currentData(clampedIntegral));
    delay(5, SECONDS);
    assertEquals(polynomial(10, -1), currentData(clampedIntegral));
  }

  @Test
  void hits_encroaching_lower_bound_then_discretely_change_integrand_without_resuming_integration() {
    set(lowerBound, polynomial(0, 1));
    set(integrandForClampedIntegrate, polynomial(-10));
    delay(5, SECONDS);
    assertEquals(polynomial(5, 1), currentData(clampedIntegral));
    set(integrandForClampedIntegrate, polynomial(0.5));
    settle();
    assertEquals(polynomial(5, 1), currentData(clampedIntegral));
    delay(5, SECONDS);
    assertEquals(polynomial(10, 1), currentData(clampedIntegral));
  }

  @Test
  void hits_upper_bound_then_discretely_change_bound_to_resume_integration() {
    delay(100, SECONDS);
    assertEquals(polynomial(100), currentData(clampedIntegral));
    set(upperBound, polynomial(200));
    settle();
    assertEquals(polynomial(100, 1), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(110, 1), currentData(clampedIntegral));
  }

  @Test
  void hits_lower_bound_then_discretely_change_bound_to_resume_integration() {
    set(integrandForClampedIntegrate, polynomial(-1));
    delay(10, SECONDS);
    assertEquals(polynomial(0), currentData(clampedIntegral));
    set(lowerBound, polynomial(-100));
    settle();
    assertEquals(polynomial(0, -1), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(-10, -1), currentData(clampedIntegral));
  }

  @Test
  void hits_upper_bound_then_discretely_change_bound_without_resuming_integration() {
    delay(100, SECONDS);
    assertEquals(polynomial(100), currentData(clampedIntegral));
    set(upperBound, polynomial(90));
    settle();
    assertEquals(polynomial(90), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(90), currentData(clampedIntegral));
  }

  @Test
  void hits_lower_bound_then_discretely_change_bound_without_resuming_integration() {
    set(integrandForClampedIntegrate, polynomial(-1));
    delay(10, SECONDS);
    assertEquals(polynomial(0), currentData(clampedIntegral));
    set(lowerBound, polynomial(10));
    settle();
    assertEquals(polynomial(10), currentData(clampedIntegral));
    delay(10, SECONDS);
    assertEquals(polynomial(10), currentData(clampedIntegral));
  }

  @Test
  void hits_upper_bound_which_then_retreats_continuously() {
    set(upperBound, polynomial(7, -2, 1));
    set(integrandForClampedIntegrate, polynomial(2));
    // With quadratics, the exact numbers have a bit of error.
    // Allow the equivalent of ~ 1 microsecond of error, 1e-6.
    // After 0.5s, integral is still below upper bound
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(6 - currentValue(clampedIntegral)) < 1e-6);
    // After 1.0s, integral has hit upper bound, which shrinks to 6
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(6 - currentValue(clampedIntegral)) < 1e-6);
    // After 1.5s, integral is still riding upper bound
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(6.25 - currentValue(clampedIntegral)) < 1e-6);
    // After 2.0s, integral leaves upper bound again
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(7 - currentValue(clampedIntegral)) < 1e-6);
    delay(1, SECONDS);
    assertTrue(Math.abs(9 - currentValue(clampedIntegral)) < 1e-6);
    delay(1, SECONDS);
    assertTrue(Math.abs(11 - currentValue(clampedIntegral)) < 1e-6);
  }

  @Test
  void hits_lower_bound_which_then_retreats_continuously() {
    set(lowerBound, polynomial(3, 2, -1));
    set(integrandForClampedIntegrate, polynomial(-2));
    // With quadratics, the exact numbers have a bit of error.
    // Allow the equivalent of ~ 1 microsecond of error, 1e-6.
    // After 0.5s, integral is still above lower bound
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(4 - currentValue(clampedIntegral)) < 1e-6);
    // After 1.0s, integral has hit lower bound, which shrinks to 6
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(4 - currentValue(clampedIntegral)) < 1e-6);
    // After 1.5s, integral is still riding lower bound
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(3.75 - currentValue(clampedIntegral)) < 1e-6);
    // After 2.0s, integral leaves lower bound again
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(3 - currentValue(clampedIntegral)) < 1e-6);
    delay(1, SECONDS);
    assertTrue(Math.abs(1 - currentValue(clampedIntegral)) < 1e-6);
    delay(1, SECONDS);
    assertTrue(Math.abs(-1 - currentValue(clampedIntegral)) < 1e-6);
  }

  @Test
  void integrand_changes_continuously_to_retreat_from_upper_bound() {
    set(integrandForClampedIntegrate, polynomial(-10));
    set(upperBound, polynomial(1));
    delay(10, SECONDS);
    set(integrandForClampedIntegrate, polynomial(2, -1));
    // With quadratics, the exact numbers have a bit of error.
    // Allow the equivalent of ~ 1 microsecond of error, 1e-6.
    assertTrue(Math.abs(0 - currentValue(clampedIntegral)) < 1e-6);
    // After .25s, integral is still within bounds
    delay(250, MILLISECONDS);
    assertTrue(Math.abs(0.46875 - currentValue(clampedIntegral)) < 1e-6);
    // After .5s, integral is still within bounds
    delay(250, MILLISECONDS);
    assertTrue(Math.abs(0.875 - currentValue(clampedIntegral)) < 1e-6);
    // After 1s, integral is clamped at maximum value
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(1 - currentValue(clampedIntegral)) < 1e-6);
    // After 2s, integral is clamped at maximum value and integrand is flipping sign
    delay(1, SECONDS);
    assertTrue(Math.abs(1 - currentValue(clampedIntegral)) < 1e-6);
    // After 2.5s, integral is retreating from upper bound.
    // Notice this is different from clamp(integrate(...), ...) in how it "exits" being clamped.
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(0.875 - currentValue(clampedIntegral)) < 1e-6);
    // After 3s, integral is still within bounds
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(0.5 - currentValue(clampedIntegral)) < 1e-6);
    // After 4s, integral is clamped at minimum value
    delay(1, SECONDS);
    assertTrue(Math.abs(0 - currentValue(clampedIntegral)) < 1e-6);
    // After 5s, integral is clamped at minimum value
    delay(1, SECONDS);
    assertTrue(Math.abs(0 - currentValue(clampedIntegral)) < 1e-6);
  }

  @Test
  void integrand_changes_continuously_to_retreat_from_lower_bound() {
    set(upperBound, polynomial(1));
    delay(10, SECONDS);
    set(integrandForClampedIntegrate, polynomial(-2, 1));
    // With quadratics, the exact numbers have a bit of error.
    // Allow the equivalent of ~ 1 microsecond of error, 1e-6.
    assertTrue(Math.abs(1 - currentValue(clampedIntegral)) < 1e-6);
    // After .25s, integral is still within bounds
    delay(250, MILLISECONDS);
    assertTrue(Math.abs(0.53125 - currentValue(clampedIntegral)) < 1e-6);
    // After .5s, integral is still within bounds
    delay(250, MILLISECONDS);
    assertTrue(Math.abs(0.125 - currentValue(clampedIntegral)) < 1e-6);
    // After 1s, integral is clamped at minimum value
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(0 - currentValue(clampedIntegral)) < 1e-6);
    // After 2s, integral is clamped at minimum value and integrand is flipping sign
    delay(1, SECONDS);
    assertTrue(Math.abs(0 - currentValue(clampedIntegral)) < 1e-6);
    // After 2.5s, integral is retreating from lower bound.
    // Notice this is different from clamp(integrate(...), ...) in how it "exits" being clamped.
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(0.125 - currentValue(clampedIntegral)) < 1e-6);
    // After 3s, integral is still within bounds
    delay(500, MILLISECONDS);
    assertTrue(Math.abs(0.5 - currentValue(clampedIntegral)) < 1e-6);
    // After 4s, integral is clamped at maximum value
    delay(1, SECONDS);
    assertTrue(Math.abs(1 - currentValue(clampedIntegral)) < 1e-6);
    // After 5s, integral is clamped at maximum value
    delay(1, SECONDS);
    assertTrue(Math.abs(1 - currentValue(clampedIntegral)) < 1e-6);
  }

  /**
   * Delay several simulation ticks without progressing time.
   * Used to allow daemon tasks to react to conditions and for values to "settle".
   */
  private static void settle() {
    delay(ZERO);
    delay(ZERO);
  }
}
