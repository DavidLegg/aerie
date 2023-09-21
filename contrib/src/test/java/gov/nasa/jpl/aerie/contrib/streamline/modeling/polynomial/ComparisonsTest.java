package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.*;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.*;

@Nested
@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ComparisonsTest {
  public ComparisonsTest(final Registrar registrar) {
    Resources.init();
  }

  private final CellResource<Polynomial> p = cellResource(polynomial(0));
  private final CellResource<Polynomial> q = cellResource(polynomial(0));

  @Test
  void comparing_distinct_constants() {
    check_comparison(lessThan(constant(0), constant(1)), true, false);
    check_comparison(lessThanOrEquals(constant(0), constant(1)), true, false);
    check_comparison(greaterThan(constant(0), constant(1)), false, false);
    check_comparison(greaterThanOrEquals(constant(0), constant(1)), false, false);
  }

  @Test
  void comparing_equal_constants() {
    check_comparison(lessThan(constant(1), constant(1)), false, false);
    check_comparison(lessThanOrEquals(constant(1), constant(1)), true, false);
    check_comparison(greaterThan(constant(1), constant(1)), false, false);
    check_comparison(greaterThanOrEquals(constant(1), constant(1)), true, false);
  }

  @Test
  void comparing_diverging_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(1, 2));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), false, false);
  }

  @Test
  void comparing_converging_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(2, -1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, true);
    check_comparison(lessThanOrEquals(p, q), true, true);
    check_comparison(greaterThan(p, q), false, true);
    check_comparison(greaterThanOrEquals(p, q), false, true);
  }

  @Test
  void comparing_equal_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(0, 1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), false, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), true, false);
  }

  @Test
  void comparing_equal_then_diverging_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(0, 2));
      delay(ZERO);
    });
    // Notice that LT is initially false, but will immediately cross over
    check_comparison(lessThan(p, q), false, true);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    // Notice that GTE is initially true, but will immediately cross over
    check_comparison(greaterThanOrEquals(p, q), true, true);
  }

  @Test
  void comparing_diverging_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1, 1));
      set(q, polynomial(1, 2, 2));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), false, false);
  }

  @Test
  void comparing_converging_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1, 1));
      set(q, polynomial(1, 2, -1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, true);
    check_comparison(lessThanOrEquals(p, q), true, true);
    check_comparison(greaterThan(p, q), false, true);
    check_comparison(greaterThanOrEquals(p, q), false, true);
  }

  @Test
  void comparing_equal_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(1, 2, -1));
      set(q, polynomial(1, 2, -1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), false, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), true, false);
  }

  @Test
  void comparing_equal_then_diverging_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(1, 2, -1));
      set(q, polynomial(1, 2, 1));
      delay(ZERO);
    });
    // Notice that LT is initially false, but will immediately cross over
    check_comparison(lessThan(p, q), false, true);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    // Notice that GTE is initially true, but will immediately cross over
    check_comparison(greaterThanOrEquals(p, q), true, true);
  }

  private void check_comparison(Resource<Discrete<Boolean>> result, boolean expectedValue, boolean expectCrossover) {
    reset();
    var resultDynamics = result.getDynamics().getOrThrow();
    assertEquals(expectedValue, resultDynamics.data().extract());
    assertEquals(expectCrossover, !resultDynamics.expiry().isNever());
    if (expectCrossover) {
      Duration crossover = resultDynamics.expiry().value().get();
      delay(crossover.minus(EPSILON));
      assertEquals(expectedValue, currentValue(result));
      delay(EPSILON);
      assertEquals(!expectedValue, currentValue(result));
    }
  }

  // Helper utilities to reset the simulation during a test.
  // This is helpful to group similar test cases within a single method,
  // even though the simulation can advance while running assertions.
  private Runnable setupFunction = () -> {};
  private void setup(Runnable setupFunction) {
    this.setupFunction = setupFunction;
    reset();
  }
  private void reset() {
    setupFunction.run();
  }
}
