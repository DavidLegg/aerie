package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static org.junit.jupiter.api.Assertions.*;

@Nested
@ExtendWith(MerlinExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DependenciesTest {
  // Private unique token, used to detect when things are anonymous
  private static final String ANON = "<ANONYMOUS>";

  Resource<Discrete<Boolean>> constantTrue = DiscreteResources.constant(true);
  Resource<Polynomial> constant1234 = constant(1234);
  Resource<Polynomial> constant5678 = constant(5678);
  Resource<Polynomial> polynomialCell = cellResource(polynomial(1));
  Resource<Polynomial> derived = map(constantTrue, constant1234, constant5678,
                                     (b, x, y) -> b.extract() ? x : y);
  Resource<Polynomial> derived_2 = bind(constantTrue, b -> b.extract() ? constant1234 : constant5678);
  Resource<Polynomial> derived_by_applicative_1 =
      rapply1(constant5678,
              rapply1(constant1234,
                      rapply1(constantTrue,
                              unit(b -> x -> y -> b.extract() ? x : y))));
  Resource<Polynomial> derived_by_applicative_2 =
      rapply2(constant5678,
              rapply2(constant1234,
                      rapply2(constantTrue,
                              unit(b -> x -> y -> b.extract() ? x : y))));
  Resource<Polynomial> derived_by_applicative_3 =
      rapply3(constant5678,
              rapply3(constant1234,
                      rapply3(constantTrue,
                              unit(b -> x -> y -> b.extract() ? x : y))));

  @Test
  void constants_are_named_by_their_value() {
    assertTrue(Naming.getName(constantTrue).get().contains("true"));
    assertTrue(Naming.getName(constant1234).get().contains("1234"));
    assertTrue(Naming.getName(constant5678).get().contains("5678"));
  }

  @Test
  void cell_resources_are_not_inherently_named() {
    assertTrue(Naming.getName(polynomialCell).isEmpty());
  }

  @Test
  void derived_resources_are_not_inherently_named() {
    assertTrue(Naming.getName(derived).isEmpty());
  }

  @Test
  void constants_have_no_dependencies() {
    assertTrue(Dependencies.getDependencies(constantTrue).isEmpty());
    assertTrue(Dependencies.getDependencies(constant1234).isEmpty());
    assertTrue(Dependencies.getDependencies(constant5678).isEmpty());
  }

  @Test
  void cell_resources_have_no_dependencies() {
    assertTrue(Dependencies.getDependencies(polynomialCell).isEmpty());
  }

  @Test
  void derived_resources_have_their_sources_as_dependencies() {
    derived.getDynamics();
    var dependencies = Dependencies.getDependencies(derived);
    assertEquals(3, dependencies.size());
    assertTrue(dependencies.contains(constantTrue));
    assertTrue(dependencies.contains(constant1234));
    assertTrue(dependencies.contains(constant5678));
  }
}
