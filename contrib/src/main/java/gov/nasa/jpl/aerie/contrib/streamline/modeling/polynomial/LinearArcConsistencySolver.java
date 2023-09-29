package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiry;
import gov.nasa.jpl.aerie.contrib.streamline.core.Reactions;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.merlin.framework.Condition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.success;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.GeneralConstraint.constraint;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.InequalityComparison.GreaterThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.InequalityComparison.LessThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.subtract;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Special methods for setting up a substepping resource solver
 * using linear constraints and arc consistency.
 *
 * <p>
 *   Constraints are linear in a set of variables,
 *   and each variable is a polynomial resource.
 *   When a driving variable changes, or the current solution expires,
 *   the solver runs independently of Aerie
 *   like a substep of the Aerie simulation step.
 * </p>
 */
public final class LinearArcConsistencySolver {
  private final String name;
  private final List<Resource<Polynomial>> drivenTerms = new LinkedList<>();
  private final List<Variable> variables = new LinkedList<>();
  private final List<DirectionalConstraint> constraints = new LinkedList<>();
  private final Map<Variable, Set<DirectionalConstraint>> neighboringConstraints = new HashMap<>();

  public LinearArcConsistencySolver(String name) {
    this.name = name;

    spawn(() -> {
      // Don't solve for the first time until sim starts.
      // This ensures all variables are initialized and all constraints are declared.
      buildNeighboringConstraints();
      solve();
      // After that, solve whenever any of the driven terms change
      // OR a solved variable changes (which can only happen when it expires)
      Reactions.whenever(
          trace("Repeat Solve", () -> Stream.concat(
              drivenTerms.stream(),
              variables.stream().map(Variable::resource))
                      .map(Resources::dynamicsChange)
                      .reduce(Condition.FALSE, (c1, c2) -> c1.or(c2))),
          this::solve);
    });
  }

  public Variable variable(String name, Function<Domain, Expiring<Polynomial>> selectionPolicy) {
    // TODO: make selectionPolicy (and maybe starting value?) configurable
    var variable = new Variable(name, cellResource(polynomial(0)), selectionPolicy);
    variables.add(variable);
    return variable;
  }

  public void declare(LinearExpression left, Comparison comparison, LinearExpression right) {
    declare(constraint(left, comparison, right));
  }

  public void declare(GeneralConstraint constraint) {
    var normalizedConstraint = constraint.normalize();
    drivenTerms.add(normalizedConstraint.drivenTerm);
    constraints.addAll(normalizedConstraint.standardize());
  }

  private void buildNeighboringConstraints() {
    for (var variable : variables) {
      neighboringConstraints.put(variable, new HashSet<>());
    }
    for (var constraint : constraints) {
      for (var drivingVariable : constraint.drivingVariables) {
        neighboringConstraints.get(drivingVariable).add(constraint);
      }
    }
  }

  private void solve() {
    System.out.println("DEBUG: solving...");
    final var domains = variables.stream().collect(toMap(identity(), VariableDomain::new));
    final Queue<DirectionalConstraint> constraintsLeft = new LinkedList<>(constraints);
    DirectionalConstraint constraint;
    try {
      // While we either have constraints to apply or domains to solve...
      while (!constraintsLeft.isEmpty() || domains.values().stream().anyMatch(VariableDomain::isUnsolved)) {
        // Apply all constraints through simple arc consistency
        while ((constraint = constraintsLeft.poll()) != null) {
          var D = domains.get(constraint.constrainedVariable.variable);
          var k = constraint.constrainedVariable.derivativeOrder;
          var newBound = constraint.bound.apply(domains).getDynamics().getOrThrow();
          boolean domainChanged = switch (constraint.comparison) {
            case LessThanOrEquals -> D.derivativeDomain(k).restrictUpper(newBound);
            case GreaterThanOrEquals -> D.derivativeDomain(k).restrictLower(newBound);
          };
          if (domainChanged) {
            System.out.printf(
                "DEBUG: Used %s^(%d) %s %s to restrict to %s%n",
                constraint.constrainedVariable.variable, k, constraint.comparison, newBound, D);
            if (D.isEmpty()) {
              throw new IllegalStateException(
                  "LinearArcConsistencySolver %s failed. Domain for %s is empty: [%s, %s]".formatted(
                      name, D.variable, D.lowerBound, D.upperBound));
            }
            constraintsLeft.addAll(neighboringConstraints.get(D.variable));
          }
        }
        // If that didn't fully solve all variables, choose the first unsolved variable
        // and use the selection policy to pick a solution arbitrarily, then restart arc consistency.
        variables
            .stream()
            .map(domains::get)
            .filter(VariableDomain::isUnsolved)
            .findFirst()
            .ifPresent(D -> {
              D.lowerBound = D.upperBound = D.variable.selectionPolicy.apply(D);
              System.out.printf("DEBUG: Stalled. Selecting %s = %s%n", D.variable, D.lowerBound);
              constraintsLeft.addAll(neighboringConstraints.get(D.variable));
            });
      }
      // All domains are solved and non-empty, emit solution
      // Expiry for entire solution is taken as a whole:
      Expiry solutionExpiry = variables
          .stream()
          .map(v -> {
            var D = domains.get(v);
            return D.lowerBound.expiry().or(D.upperBound.expiry());
          })
          .reduce(Expiry.NEVER, Expiry::or);
//      set(this.solutionExpiry, solutionExpiry); // DEBUG
      for (var v : variables) {
        // Overwrite failures if we recover
        var result = success(expiring(domains.get(v).lowerBound.data(), solutionExpiry));
        v.resource.emit($ -> result);
        System.out.printf("DEBUG: %s - Solved %s = %s%n", Resources.currentTime(), v, result);
      }
    } catch (Exception e) {
      // Solving failed, so populate all outputs with the failure.
      ErrorCatching<Expiring<Polynomial>> result = failure(e);
      for (var v : variables) {
        // Don't emit failures on cells that have already failed, though.
        // That would make those cells unnecessarily noisy.
        if (!(v.resource.getDynamics() instanceof ErrorCatching.Failure<Expiring<Polynomial>>)) {
          v.resource.emit($ -> result);
        }
      }
    }
  }

  public static final class Variable {
    private final String name;
    private final CellResource<Polynomial> resource;
    private final Function<Domain, Expiring<Polynomial>> selectionPolicy;

    public Variable(
        String name,
        CellResource<Polynomial> resource,
        Function<Domain, Expiring<Polynomial>> selectionPolicy) {
      this.name = name;
      this.resource = resource;
      this.selectionPolicy = selectionPolicy;
    }

    @Override
    public String toString() {
      return name;
    }

    // Expose resource as Resource, not CellResource,
    // because only the solver should emit effects on it.
    public Resource<Polynomial> resource() {
      return resource;
    }
  }

  public enum Comparison {
    LessThanOrEquals,
    GreaterThanOrEquals,
    Equals;
  }
  public enum InequalityComparison {
    LessThanOrEquals,
    GreaterThanOrEquals;

    InequalityComparison opposite() {
      return switch (this) {
        case LessThanOrEquals -> GreaterThanOrEquals;
        case GreaterThanOrEquals -> LessThanOrEquals;
      };
    }
  }

  public record VariableDerivative(Variable variable, int derivativeOrder) {}
  /**
   * Expression drivenTerm + sum of c_i * s_i over entries c_i -> s_i in controlledTerm
   */
  public record LinearExpression(Resource<Polynomial> drivenTerm, Map<VariableDerivative, Double> controlledTerm) {
    public static LinearExpression lx(double value) {
      return lx(constant(value));
    }
    public static LinearExpression lx(Resource<Polynomial> drivenTerm) {
      return new LinearExpression(drivenTerm, Map.of());
    }
    public static LinearExpression lx(Variable controlledTerm) {
      return new LinearExpression(constant(0), Map.of(new VariableDerivative(controlledTerm, 0), 1.0));
    }
    public LinearExpression add(LinearExpression other) {
      return new LinearExpression(
          PolynomialResources.add(drivenTerm, other.drivenTerm),
          addControlledTerms(controlledTerm, other.controlledTerm));
    }
    public LinearExpression subtract(LinearExpression other) {
      return this.add(other.multiply(-1));
    }
    public LinearExpression multiply(double scale) {
      if (scale == 0) {
        // Short circuit to avoid unnecessary dependencies.
        return lx(constant(0));
      } else {
        return new LinearExpression(
            PolynomialResources.multiply(drivenTerm, constant(scale)),
            scaleControlledTerm(controlledTerm, scale));
      }
    }
    public LinearExpression derivative() {
      return new LinearExpression(
          PolynomialResources.differentiate(drivenTerm),
          differentiateControlledTerm(controlledTerm, 1));
    }

    /**
     * Integral of this expression.
     * Undoes derivative exactly for variables,
     * but assumes a starting value of 0 otherwise.
     *
     * @see LinearExpression#integral(Resource)
     */
    public LinearExpression integral() {
      return new LinearExpression(
          PolynomialResources.integrate(drivenTerm, 0),
          differentiateControlledTerm(controlledTerm, -1));
    }

    /**
     * Convenience constructor that takes the resource representing this integral.
     * Extracts the current value of this resource to maintain continuity of the integral
     * across multiple solver iterations.
     */
    public LinearExpression integral(Resource<Polynomial> integralValue) {
      // To allow the solver to re-solve for integral dynamics, erase higher-order coefficients and expiry info.
      return this.integral().add(lx(
          () -> integralValue.getDynamics().map(
              e -> neverExpiring(polynomial(e.data().extract())))));
    }

    private Map<VariableDerivative, Double> scaleControlledTerm(Map<VariableDerivative, Double> controlledTerm, double scale) {
      var result = new HashMap<>(controlledTerm);
      for (var v : result.keySet()) {
        result.computeIfPresent(v, (v$, s) -> s * scale);
      }
      return result;
    }

    private static Map<VariableDerivative, Double> addControlledTerms(Map<VariableDerivative, Double> left, Map<VariableDerivative, Double> right) {
      var result = new HashMap<VariableDerivative, Double>();
      var allVariables = new HashSet<>(left.keySet());
      allVariables.addAll(right.keySet());
      for (var v : allVariables) {
        double scale = left.getOrDefault(v, 0.0) + right.getOrDefault(v, 0.0);
        if (scale != 0.0) {
          result.put(v, scale);
        }
      }
      return result;
    }

    private static Map<VariableDerivative, Double> differentiateControlledTerm(Map<VariableDerivative, Double> controlledTerm, int order) {
      var result = new HashMap<VariableDerivative, Double>();
      for (var entry : controlledTerm.entrySet()) {
        var v = entry.getKey();
        result.put(new VariableDerivative(v.variable, v.derivativeOrder + order), entry.getValue());
      }
      return result;
    }
  }

  // The following three kinds of constraints are equivalent, but are best suited to different use cases.
  // General is easiest to read and write in model code, as it's the most flexible.
  public record GeneralConstraint(LinearExpression left, Comparison comparison, LinearExpression right) {
    NormalizedConstraint normalize() {
      var drivenTerm = subtract(right.drivenTerm, left.drivenTerm);
      var controlledTerm = new HashMap<VariableDerivative, Double>();
      var allVariables = new HashSet<>(left.controlledTerm().keySet());
      allVariables.addAll(right.controlledTerm().keySet());
      for (var v : allVariables) {
        double scale = left.controlledTerm().getOrDefault(v, 0.0) - right.controlledTerm().getOrDefault(v, 0.0);
        if (scale != 0.0) {
          controlledTerm.put(v, scale);
        }
      }
      return new NormalizedConstraint(controlledTerm, comparison, drivenTerm);
    }

    public static GeneralConstraint constraint(LinearExpression left, Comparison comparison, LinearExpression right) {
      return new GeneralConstraint(left, comparison, right);
    }
  }
  // Normalized is like General without redundant information. Also, drivenTerm can be used to trigger solving.
  private record NormalizedConstraint(
      Map<VariableDerivative, Double> controlledTerm,
      Comparison comparison,
      Resource<Polynomial> drivenTerm) {
    List<DirectionalConstraint> standardize() {
      return controlledTerm.keySet().stream().flatMap(this::directionalConstraints).toList();
    }
    private Stream<DirectionalConstraint> directionalConstraints(VariableDerivative constrainedVariable) {
      double inverseScale = 1 / controlledTerm.get(constrainedVariable);
      var drivingVariables = new HashSet<>(controlledTerm.keySet());
      drivingVariables.remove(constrainedVariable);
      Stream<InequalityComparison> inequalityComparisons = switch (comparison) {
        case LessThanOrEquals -> Stream.of(InequalityComparison.LessThanOrEquals);
        case GreaterThanOrEquals -> Stream.of(GreaterThanOrEquals);
        case Equals -> Stream.of(InequalityComparison.LessThanOrEquals, GreaterThanOrEquals);
      };
      return inequalityComparisons.map(c -> new DirectionalConstraint(constrainedVariable, inverseScale > 0 ? c : c.opposite(), domains -> {
        // Expiry for driven terms is captured by re-solving rather than expiring the solution.
        // If solver has a feedback loop from last iteration (which is common)
        // feeding that expiry in here can loop the solver forever.
        var result = eraseExpiry(drivenTerm);
        for (var drivingVariable : drivingVariables) {
          var scale = controlledTerm.get(drivingVariable);
          var domain = domains.get(drivingVariable.variable).derivativeDomain(drivingVariable.derivativeOrder);
          var useLowerBound = (scale > 0) == (c == LessThanOrEquals);
          var domainBound = ExpiringMonad.map(
              useLowerBound ? domain.lowerBound() : domain.upperBound(),
              b -> b.multiply(polynomial(-scale)));
          result = add(result, () -> success(domainBound));
        }
        return multiply(result, constant(inverseScale));
      }, drivingVariables.stream().map(VariableDerivative::variable).collect(Collectors.toSet())));
    }
    private static <D> Resource<D> eraseExpiry(Resource<D> p) {
      return () -> p.getDynamics().map(e -> neverExpiring(e.data()));
    }
  }
  // Directional constraints are useful for arc consistency, since they have input (driving) and output (constrained) variables.
  // However, many directional constraints are required in general to express one General constraint.
  private record DirectionalConstraint(VariableDerivative constrainedVariable, InequalityComparison comparison, Function<Map<Variable, ? extends Domain>, Resource<Polynomial>> bound, Set<Variable> drivingVariables) {}

  public interface Domain {
    Expiring<Polynomial> lowerBound();
    Expiring<Polynomial> upperBound();
    boolean restrictLower(Expiring<Polynomial> newLowerBound);
    boolean restrictUpper(Expiring<Polynomial> newUpperBound);
    default Domain derivativeDomain(int order) {
      Domain result = this;
      // while (order-- > 0) result = result.derivative();
      for (; order > 0; --order) result = result.derivative();
      for (; order < 0; ++order) result = result.integral();
      return result;
    }
    default Domain derivative() {
      return new Domain() {
        @Override
        public Expiring<Polynomial> lowerBound() {
          return valueClamped()
              ? ExpiringMonad.map(Domain.this.lowerBound(), Polynomial::derivative)
              : neverExpiring(polynomial(Double.NEGATIVE_INFINITY));
        }

        @Override
        public Expiring<Polynomial> upperBound() {
          return valueClamped()
              ? ExpiringMonad.map(Domain.this.upperBound(), Polynomial::derivative)
              : neverExpiring(polynomial(Double.POSITIVE_INFINITY));
        }

        @Override
        public boolean restrictLower(final Expiring<Polynomial> newLowerBound) {
          // If value is unclamped, restricting the derivative means nothing.
          return valueClamped() && Domain.this.restrictLower(
              ExpiringMonad.map(newLowerBound, lb -> lb.integral(Domain.this.lowerBound().data().extract())));
        }

        @Override
        public boolean restrictUpper(final Expiring<Polynomial> newUpperBound) {
          // If value is unclamped, restricting the derivative means nothing.
          return valueClamped() && Domain.this.restrictUpper(
              ExpiringMonad.map(newUpperBound, lb -> lb.integral(Domain.this.upperBound().data().extract())));
        }

        private boolean valueClamped() {
          return Objects.equals(
              Domain.this.lowerBound().data().extract(),
              Domain.this.upperBound().data().extract());
        }
      };
    }
    default Domain integral() {
      return new Domain() {
        @Override
        public Expiring<Polynomial> lowerBound() {
          return ExpiringMonad.map(Domain.this.lowerBound(), p -> p.integral(0));
        }

        @Override
        public Expiring<Polynomial> upperBound() {
          return ExpiringMonad.map(Domain.this.upperBound(), p -> p.integral(0));
        }

        @Override
        public boolean restrictLower(final Expiring<Polynomial> newLowerBound) {
          if (newLowerBound.data().extract() > 0) {
            // This domain has value 0, so now it should be empty.
            return Domain.this.restrictLower(neverExpiring(polynomial(Double.POSITIVE_INFINITY)));
          } else if (newLowerBound.data().extract() == 0) {
            // Nominal case
            return Domain.this.restrictLower(ExpiringMonad.map(newLowerBound, Polynomial::derivative));
          } else {
            // integral value is 0, so dominates any negative-value lower bound.
            return false;
          }
        }

        @Override
        public boolean restrictUpper(final Expiring<Polynomial> newUpperBound) {
          if (newUpperBound.data().extract() < 0) {
            // This domain has value 0, so now it should be empty.
            return Domain.this.restrictUpper(neverExpiring(polynomial(Double.NEGATIVE_INFINITY)));
          } else if (newUpperBound.data().extract() == 0) {
            // Nominal case
            return Domain.this.restrictUpper(ExpiringMonad.map(newUpperBound, Polynomial::derivative));
          } else {
            // integral value is 0, so dominates any negative-value lower bound.
            return false;
          }
        }
      };
    }
    default boolean isEmpty() {
      return lowerBound().data().extract() > upperBound().data().extract();
    }
    default boolean isUnsolved() {
      return !lowerBound().data().equals(upperBound().data());
    }
  }

  public static final class VariableDomain implements Domain {
    public final Variable variable;
    private Expiring<Polynomial> lowerBound;
    private Expiring<Polynomial> upperBound;

    public VariableDomain(Variable variable) {
      this.variable = variable;
      this.lowerBound = neverExpiring(polynomial(Double.NEGATIVE_INFINITY));
      this.upperBound = neverExpiring(polynomial(Double.POSITIVE_INFINITY));
    }

    @Override
    public Expiring<Polynomial> lowerBound() {
      return lowerBound;
    }

    @Override
    public Expiring<Polynomial> upperBound() {
      return upperBound;
    }

    @Override
    public boolean restrictLower(Expiring<Polynomial> newLowerBound) {
      var oldLowerBound = lowerBound;
      lowerBound = bind(lowerBound, ub -> bind(newLowerBound, ub::max));
      return !lowerBound.equals(oldLowerBound);
    }

    @Override
    public boolean restrictUpper(Expiring<Polynomial> newUpperBound) {
      var oldUpperBound = upperBound;
      upperBound = bind(upperBound, ub -> bind(newUpperBound, ub::min));
      return !upperBound.equals(oldUpperBound);
    }

    @Override
    public String toString() {
      return "Domain[" + lowerBound + ", " + upperBound + ']';
    }
  }
}
