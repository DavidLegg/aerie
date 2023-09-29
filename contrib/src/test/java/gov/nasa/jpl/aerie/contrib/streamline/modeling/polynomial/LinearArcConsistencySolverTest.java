package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiry;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.Domain;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.Comparison.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.LinearExpression.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.*;

class LinearArcConsistencySolverTest {
  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class SingleVariableSingleConstraint {
    CellResource<Polynomial> driver = cellResource(polynomial(10));
    Resource<Polynomial> result;

    public SingleVariableSingleConstraint() {
      Resources.init();

      var solver = new LinearArcConsistencySolver("SingleVariableSingleConstraint");
      var v = solver.variable("v", Domain::upperBound);
      result = v.resource();
      solver.declare(lx(v), LessThanOrEquals, lx(driver));
    }

    @Test
    void initial_results_are_ready_after_settling() {
      settle();
      assertEquals(polynomial(10), currentData(result));
    }

    @Test
    void solver_reacts_to_driving_resource() {
      set(driver, polynomial(20, -1, 3));
      settle();
      assertEquals(polynomial(20, -1, 3), currentData(result));
    }

    @Test
    void results_evolve_with_time() {
      set(driver, polynomial(20, -1, 3));
      settle();
      assertEquals(polynomial(20, -1, 3), currentData(result));
      delay(10, SECONDS);
      // new dynamics = 20 - 1 (x + 10) + 3 (x + 10)^2 = 310 + 59x + 3x^2
      assertEquals(polynomial(310, 59, 3), currentData(result));
    }
  }

  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class SingleVariableMultipleConstraint {
    CellResource<Polynomial> lowerBound1 = cellResource(polynomial(10));
    CellResource<Polynomial> lowerBound2 = cellResource(polynomial(20));
    CellResource<Polynomial> upperBound = cellResource(polynomial(30));
    Resource<Polynomial> result;

    public SingleVariableMultipleConstraint() {
      Resources.init();

      var solver = new LinearArcConsistencySolver("SingleVariableMultipleConstraint");
      var v = solver.variable("v", Domain::lowerBound);
      result = v.resource();
      solver.declare(lx(v), GreaterThanOrEquals, lx(lowerBound1));
      solver.declare(lx(v), GreaterThanOrEquals, lx(lowerBound2));
      solver.declare(lx(v), LessThanOrEquals, lx(upperBound));
    }

    @Test
    void initial_results_use_selection_policy() {
      settle();
      assertEquals(polynomial(20), currentData(result));
    }

    @Test
    void fully_determined_bounds_are_allowed() {
      set(lowerBound1, polynomial(10, 5));
      set(lowerBound2, polynomial(12, 3));
      set(upperBound, polynomial(12, 3));
      settle();
      assertEquals(polynomial(12, 3), currentData(result));
    }

    @Test
    void tangent_bounds_use_dominant_behavior() {
      // Although lb1 == lb2 now, lb2 has a greater slope, so it dominates
      set(lowerBound1, polynomial(12, 3, 5));
      set(lowerBound2, polynomial(12, 4, -1));
      set(upperBound, polynomial(12, 5));
      settle();
      assertEquals(polynomial(12, 4, -1), currentData(result));
    }

    @Test
    void infeasible_bounds_result_in_failure() {
      set(lowerBound1, polynomial(12, 3, 5));
      set(lowerBound2, polynomial(12, 4, -1));
      set(upperBound, polynomial(11, 7));
      settle();
      assertInstanceOf(ErrorCatching.Failure.class, result.getDynamics());
    }

    /**
     * Clearing failures when upstream conditions improve mirrors
     * the logic of derived resources, where downstream resources fail
     * only when upstream resources fail; downstream resources clear
     * when upstream resources clear and derivation succeeds.
     */
    @Test
    void failures_are_cleared_if_problem_becomes_feasible_again() {
      set(lowerBound1, polynomial(12, 3, 5));
      set(lowerBound2, polynomial(12, 4, -1));
      set(upperBound, polynomial(11, 7));
      settle();
      assertInstanceOf(ErrorCatching.Failure.class, result.getDynamics());

      set(lowerBound1, polynomial(10, 5));
      set(lowerBound2, polynomial(12, 3));
      set(upperBound, polynomial(12, 3));
      settle();
      assertEquals(polynomial(12, 3), currentData(result));
    }
  }

  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class ScalingConstraint {
    CellResource<Polynomial> driver = cellResource(polynomial(10));
    Resource<Polynomial> result;

    public ScalingConstraint() {
      Resources.init();

      var solver = new LinearArcConsistencySolver("ScalingConstraint");
      var v = solver.variable("v", Domain::upperBound);
      result = v.resource();
      solver.declare(lx(v).multiply(4), LessThanOrEquals, lx(driver));
    }

    @Test
    void scaling_can_be_inverted_when_solving() {
      settle();
      assertEquals(polynomial(2.5), currentData(result));
    }

    @Test
    void scaling_is_respected_for_later_solutions() {
      set(driver, polynomial(20, 4, -8));
      settle();
      assertEquals(polynomial(5, 1, -2), currentData(result));
    }
  }

  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class CalculusConstraint {
    CellResource<Polynomial> driver = cellResource(polynomial(10, 20, 30));
    Resource<Polynomial> derivative, integral;

    public CalculusConstraint() {
      Resources.init();

      var solver = new LinearArcConsistencySolver("CalculusConstraint");
      var v = solver.variable("v", Domain::upperBound);
      var vPrime = solver.variable("vPrime", Domain::upperBound);
      var vIntegral = solver.variable("vIntegral", Domain::upperBound);
      derivative = vPrime.resource();
      integral = vIntegral.resource();
      solver.declare(lx(v), LessThanOrEquals, lx(driver));
      solver.declare(lx(vPrime), Equals, lx(v).derivative());
      solver.declare(lx(vIntegral), Equals, lx(v).integral(integral));
    }

    @Test
    void derivative_can_be_solved_for() {
      settle();
      assertEquals(polynomial(20, 60), currentData(derivative));
    }

    @Test
    void derivative_is_respected_for_later_solutions() {
      set(driver, polynomial(20, 4, -9));
      settle();
      assertEquals(polynomial(4, -18), currentData(derivative));
    }

    @Test
    void integral_can_be_sovled_for() {
      settle();
      assertEquals(polynomial(0, 10, 10, 10), currentData(integral));
    }

    @Test
    void integral_is_respected_for_later_solutions() {
      set(driver, polynomial(20, 4, -9));
      settle();
      assertEquals(polynomial(0, 20, 2, -3), currentData(integral));
    }

    @Test
    void integral_is_continuous_across_solver_iterations() {
      settle();
      assertEquals(polynomial(0, 10, 10, 10), currentData(integral));
      delay(SECOND);
      // 0 + 10(x + 1) + 10(x + 1)^2 + 10(x + 1)^3 = 30 + 60x + 40x^2 + 10x^3
      assertEquals(polynomial(30, 60, 40, 10), currentData(integral));
      set(driver, polynomial(20, 4, -9));
      settle();
      // Notice high-order coefficients are completely determined by driver,
      // but value is completely determined by current value.
      assertEquals(polynomial(30, 20, 2, -3), currentData(integral));
    }
  }

  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class MultipleVariables {
    CellResource<Polynomial> upperBound = cellResource(polynomial(10));
    CellResource<Polynomial> upperBoundOnC = cellResource(polynomial(5));
    Resource<Polynomial> a, b, c;

    public MultipleVariables() {
      Resources.init();

      var solver = new LinearArcConsistencySolver("MultipleVariablesSingleConstraint");
      var a = solver.variable("a", Domain::upperBound);
      var b = solver.variable("b", Domain::upperBound);
      var c = solver.variable("c", Domain::upperBound);
      this.a = a.resource();
      this.b = b.resource();
      this.c = c.resource();
      solver.declare(lx(a).add(lx(b).multiply(2)).subtract(lx(c)), LessThanOrEquals, lx(upperBound));
      solver.declare(lx(c), LessThanOrEquals, lx(upperBoundOnC));
      solver.declare(lx(a), GreaterThanOrEquals, lx(0));
      solver.declare(lx(b), GreaterThanOrEquals, lx(0));
      solver.declare(lx(c), GreaterThanOrEquals, lx(0));
    }

    @Test
    void when_problem_is_underconstrained_variables_are_resolved_in_declaration_order() {
      settle();
      // Since a is resolved first, it chooses the greatest value it can
      assertEquals(polynomial(15), currentData(a));
      // b and c are determined as a result of this. Notice b is constrained all the way down to 0.
      assertEquals(polynomial(0), currentData(b));
      assertEquals(polynomial(5), currentData(c));
    }

    @Test
    void when_problem_is_fully_determined_the_solution_is_reached() {
      set(upperBoundOnC, polynomial(0));
      set(upperBound, polynomial(0));
      // Forces a = b = c = 0
      settle();
      assertEquals(polynomial(0), currentData(a));
      assertEquals(polynomial(0), currentData(b));
      assertEquals(polynomial(0), currentData(c));
    }

    @Test
    void solving_works_on_higher_coefficients_too() {
      set(upperBoundOnC, polynomial(5, -1));
      set(upperBound, polynomial(10, -2));
      settle();
      assertEquals(polynomial(15, -3), currentData(a));
      assertEquals(polynomial(0), currentData(b));
      assertEquals(polynomial(5, -1), currentData(c));
      // Since the problem will be infeasible at t = 5s, that should be the expiry on everything:
      assertEquals(Expiry.at(Duration.of(5, SECONDS)), a.getDynamics().getOrThrow().expiry());
      assertEquals(Expiry.at(Duration.of(5, SECONDS)), b.getDynamics().getOrThrow().expiry());
      assertEquals(Expiry.at(Duration.of(5, SECONDS)), c.getDynamics().getOrThrow().expiry());
    }
  }

  static void settle() {
    delay(ZERO);
    delay(ZERO);
  }
}
