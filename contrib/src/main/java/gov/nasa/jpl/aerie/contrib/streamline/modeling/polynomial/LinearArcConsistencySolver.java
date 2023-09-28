package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiry;
import gov.nasa.jpl.aerie.contrib.streamline.core.Reactions;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.merlin.framework.Condition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.success;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearArcConsistencySolver.GeneralConstraint.constraint;
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
          () -> Stream.concat(
              drivenTerms.stream(),
              variables.stream().map(Variable::resource))
                      .map(Resources::dynamicsChange)
                      .reduce(Condition.FALSE, (c1, c2) -> c1.or(c2)),
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
    final var domains = variables.stream().collect(toMap(identity(), Domain::new));
    final Queue<DirectionalConstraint> constraintsLeft = new LinkedList<>(constraints);
    DirectionalConstraint constraint;
    try {
      // While we either have constraints to apply or domains to solve...
      while (!constraintsLeft.isEmpty() || domains.values().stream().anyMatch(Domain::isUnsolved)) {
        // Apply all constraints through simple arc consistency
        while ((constraint = constraintsLeft.poll()) != null) {
          var D = domains.get(constraint.constrainedVariable);
          System.out.printf("DEBUG: constraining %s [%s, %s]...%n", D.variable, D.lowerBound, D.upperBound);
          var newBound = constraint.bound.apply(domains).getDynamics().getOrThrow();
          boolean domainChanged = switch (constraint.comparison) {
            case LessThanOrEqual -> D.restrictUpper(newBound);
            case GreaterThanOrEqual -> D.restrictLower(newBound);
          };
          if (domainChanged) {
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
            .filter(Domain::isUnsolved)
            .findFirst()
            .ifPresent(D -> {
              D.lowerBound = D.upperBound = D.variable.selectionPolicy.apply(D);
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
      for (var v : variables) {
        // Overwrite failures if we recover
        var result = success(expiring(domains.get(v).lowerBound.data(), solutionExpiry));
        v.resource.emit($ -> result);
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
    LessThanOrEqual,
    GreaterThanOrEqual;

    public static Comparison opposite(Comparison comparison) {
      return switch (comparison) {
        case LessThanOrEqual -> GreaterThanOrEqual;
        case GreaterThanOrEqual -> LessThanOrEqual;
      };
    }
  }

  /**
   * Expression drivenTerm + sum of c_i * s_i over entries c_i -> s_i in controlledTerm
   */
  public record LinearExpression(Resource<Polynomial> drivenTerm, Map<Variable, Double> controlledTerm) {
    public static LinearExpression lx(double value) {
      return lx(constant(value));
    }
    public static LinearExpression lx(Resource<Polynomial> drivenTerm) {
      return new LinearExpression(drivenTerm, Map.of());
    }
    public static LinearExpression lx(Variable controlledTerm) {
      return new LinearExpression(constant(0), Map.of(controlledTerm, 1.0));
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

    private Map<Variable, Double> scaleControlledTerm(Map<Variable, Double> controlledTerm, double scale) {
      var result = new HashMap<>(controlledTerm);
      for (var v : result.keySet()) {
        result.computeIfPresent(v, (v$, s) -> s * scale);
      }
      return result;
    }

    private static Map<Variable, Double> addControlledTerms(Map<Variable, Double> left, Map<Variable, Double> right) {
      var result = new HashMap<Variable, Double>();
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
  }

  // The following three kinds of constraints are equivalent, but are best suited to different use cases.
  // General is easiest to read and write in model code, as it's the most flexible.
  public record GeneralConstraint(LinearExpression left, Comparison comparison, LinearExpression right) {
    NormalizedConstraint normalize() {
      var drivenTerm = subtract(right.drivenTerm, left.drivenTerm);
      var controlledTerm = new HashMap<Variable, Double>();
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
      Map<Variable, Double> controlledTerm,
      Comparison comparison,
      Resource<Polynomial> drivenTerm) {
    List<DirectionalConstraint> standardize() {
      return controlledTerm.keySet().stream().map(this::standardConstraint).toList();
    }

    private DirectionalConstraint standardConstraint(Variable constrainedVariable) {
      double inverseScale = 1 / controlledTerm.get(constrainedVariable);
      var standardComparison = inverseScale > 0 ? comparison : Comparison.opposite(comparison);
      var drivingVariables = new HashSet<>(controlledTerm.keySet());
      drivingVariables.remove(constrainedVariable);
      return new DirectionalConstraint(constrainedVariable, standardComparison, domains -> {
        var result = drivenTerm;
        for (var drivingVariable : drivingVariables) {
          var scale = controlledTerm.get(drivingVariable);
          var domain = domains.get(drivingVariable);
          var domainBound = ExpiringMonad.map(
              scale > 0 ? domain.lowerBound : domain.upperBound,
              b -> b.multiply(polynomial(-scale)));
          result = add(result, () -> success(domainBound));
        }
        return multiply(result, constant(inverseScale));
      }, drivingVariables);
    }
  }
  // Directional constraints are useful for arc consistency, since they have input (driving) and output (constrained) variables.
  // However, many directional constraints are required in general to express one General constraint.
  private record DirectionalConstraint(Variable constrainedVariable, Comparison comparison, Function<Map<Variable, Domain>, Resource<Polynomial>> bound, Set<Variable> drivingVariables) {}

  public static final class Domain {
    public final Variable variable;
    private Expiring<Polynomial> lowerBound;
    private Expiring<Polynomial> upperBound;

    public Domain(Variable variable) {
      this.variable = variable;
      this.lowerBound = neverExpiring(polynomial(Double.NEGATIVE_INFINITY));
      this.upperBound = neverExpiring(polynomial(Double.POSITIVE_INFINITY));
    }

    public Expiring<Polynomial> lowerBound() {
      return lowerBound;
    }

    public Expiring<Polynomial> upperBound() {
      return upperBound;
    }

    public boolean restrictLower(Expiring<Polynomial> newLowerBound) {
      var oldLowerBound = lowerBound;
      lowerBound = bind(lowerBound, ub -> bind(newLowerBound, ub::max));
      return !lowerBound.equals(oldLowerBound);
    }

    public boolean restrictUpper(Expiring<Polynomial> newUpperBound) {
      var oldUpperBound = upperBound;
      upperBound = bind(upperBound, ub -> bind(newUpperBound, ub::min));
      return !upperBound.equals(oldUpperBound);
    }

    public boolean isEmpty() {
      return lowerBound.data().extract() > upperBound.data().extract();
    }

    public boolean isUnsolved() {
      return !lowerBound.data().equals(upperBound.data());
    }

    @Override
    public String toString() {
      return "Domain[" +
             "lowerBound=" + lowerBound + ", " +
             "upperBound=" + upperBound + ']';
    }
  }
}
