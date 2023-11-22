package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.expiry;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.apache.commons.math3.analysis.polynomials.PolynomialsUtils.shift;

public record Polynomial(double[] coefficients) implements Dynamics<Double, Polynomial> {
  /**
   * Maximum imaginary component allowed in a root to be considered "real" when performing root-finding.
   * Should be a very small number to avoid spurious roots.
   */
  private static final double ROOT_FINDING_IMAGINARY_COMPONENT_TOLERANCE = 1e-12;

  /**
   * Maximum number of time steps to search in either direction around near-roots
   * to find the corresponding discretized transition point.
   */
  private static final int MAX_RANGE_FOR_ROOT_SEARCH = 2;

  // TODO: Add Duration parameter for unit of formal parameter
  public static Polynomial polynomial(double... coefficients) {
    int n = coefficients.length;
    if (n == 0) {
      return new Polynomial(new double[] { 0.0 });
    }
    while (n > 1 && coefficients[n - 1] == 0) --n;
    return new Polynomial(Arrays.copyOf(coefficients, n));
  }

  @Override
  public Double extract() {
    return coefficients()[0];
  }

  @Override
  public Polynomial step(Duration t) {
    return t.isEqualTo(ZERO) ? this : polynomial(shift(coefficients(), t.ratioOver(SECOND)));
  }

  public int degree() {
    return coefficients().length - 1;
  }

  public boolean isConstant() {
    return degree() == 0;
  }

  public boolean isNonFinite() {
    return Arrays.stream(coefficients()).anyMatch(c -> !Double.isFinite(c));
  }

  public Polynomial add(Polynomial other) {
    final double[] coefficients = coefficients();
    final double[] otherCoefficients = other.coefficients();
    final int minLength = Math.min(coefficients.length, otherCoefficients.length);
    final int maxLength = Math.max(coefficients.length, otherCoefficients.length);
    final double[] newCoefficients = new double[maxLength];
    for (int i = 0; i < minLength; ++i) {
      newCoefficients[i] = coefficients[i] + otherCoefficients[i];
    }
    if (coefficients.length > minLength)
      System.arraycopy(coefficients, minLength, newCoefficients, minLength, coefficients.length - minLength);
    if (otherCoefficients.length > minLength)
      System.arraycopy(
          otherCoefficients, minLength, newCoefficients, minLength, otherCoefficients.length - minLength);
    return polynomial(newCoefficients);
  }

  public Polynomial subtract(Polynomial other) {
    return add(other.multiply(polynomial(-1)));
  }

  public Polynomial multiply(Polynomial other) {
    final double[] coefficients = coefficients();
    final double[] otherCoefficients = other.coefficients();
    // Length = degree + 1, so
    // new length = 1 + new degree
    //   = 1 + (degree + other.degree)
    //   = 1 + (length - 1 + other.length - 1)
    //   = length + other.length - 1
    final double[] newCoefficients = new double[coefficients.length + otherCoefficients.length - 1];
    for (int exponent = 0; exponent < newCoefficients.length; ++exponent) {
      newCoefficients[exponent] = 0.0;
      // 0 <= k < length and 0 <= exponent - k < other.length
      // implies k >= 0, k > exponent - other.length,
      // k < length, and k <= exponent
      for (int k = Math.max(0, exponent - otherCoefficients.length + 1);
           k < Math.min(coefficients.length, exponent + 1);
           ++k) {
        newCoefficients[exponent] += coefficients[k] * otherCoefficients[exponent - k];
      }
    }
    return polynomial(newCoefficients);
  }

  public Polynomial divide(double scalar) {
    final double[] coefficients = coefficients();
    final double[] newCoefficients = new double[coefficients.length];
    for (int i = 0; i < coefficients.length; ++i) {
      newCoefficients[i] = coefficients[i] / scalar;
    }
    return polynomial(newCoefficients);
  }

  public Polynomial integral(double startingValue) {
    final double[] coefficients = coefficients();
    final double[] newCoefficients = new double[coefficients.length + 1];
    newCoefficients[0] = startingValue;
    for (int i = 0; i < coefficients.length; ++i) {
      newCoefficients[i + 1] = coefficients[i] / (i + 1);
    }
    return polynomial(newCoefficients);
  }

  public Polynomial derivative() {
    final double[] coefficients = coefficients();
    final double[] newCoefficients = new double[coefficients.length - 1];
    for (int i = 1; i < coefficients.length; ++i) {
      newCoefficients[i - 1] = coefficients[i] * i;
    }
    return polynomial(newCoefficients);
  }

  public double evaluate(Duration t) {
    // Although there are more efficient ways to evaluate a polynomial,
    // it's *very* important to simulation stability that
    // evaluate(t) agrees exactly with step(t).extract()
    return step(t).extract();
  }

  /**
   * Find the first non-negative time when timePredicate is true
   * and this polynomial is positive or non-negative, depending on strictness.
   * This is a helper method to higher-level comparison and root-finding functions.
   *
   * <p>
   *   We can often formulate the time that interesting changes happen as roots of a polynomial.
   *   However, floating-point precision issues may mean that the polynomial we formulate,
   *   with values near 0, has higher precision than the arguments we started with,
   *   so we do an exhaustive search over the range indicated by the higher-precision formulation
   *   to find the time when the lower-precision formulation changes.
   * </p>
   */
  private Optional<Duration> findSupported(Predicate<Duration> timePredicate, boolean strict) {
    DoublePredicate supportTest = strict ? x -> x > 0 : x -> x >= 0;
    Duration root, start, end;
    try {
      if (supportTest.test(extract())) {
        root = ZERO;
      } else {
        var t$ = findFutureRoots().findFirst();
        if (t$.isEmpty()) return Optional.empty();
        root = t$.get();
      }

      // Do an exponential search to bracket the transition time
      Duration rangeSize = EPSILON;
      if (timePredicate.test(root)) {
        end = root;
        start = end.minus(rangeSize);
        while (timePredicate.test(start)) {
          rangeSize = rangeSize.times(2);
          start = end.minus(rangeSize);
        }
      } else {
        start = root;
        end = start.plus(rangeSize);
        while (!timePredicate.test(end)) {
          rangeSize = rangeSize.times(2);
          end = start.plus(rangeSize);
        }
      }
      // TODO: There's an unhandled edge case here, where timePredicate is satisfied in a period we jumped over.
      //   Maybe try to use the precision of the arguments and the finer resolution polynomial "this"
      //   to do a more thorough but still efficient search?
    } catch (ArithmeticException e) {
      // If we overflowed looking for a bracketing range, it effectively never transitions.
      return Optional.empty();
    }

    // Do a binary search to find the exact transition time
    while (end.longerThan(start.plus(EPSILON))) {
      Duration midpoint = start.plus(end).dividedBy(2);
      if (timePredicate.test(midpoint)) {
        end = midpoint;
      } else {
        start = midpoint;
      }
    }
    return Optional.of(end);
  }

  private Expiring<Discrete<Boolean>> greaterThan(Polynomial other, boolean strict) {
    BiPredicate<Double, Double> comp = strict ? (x, y) -> x > y : (x, y) -> x >= y;
    boolean result = comp.test(this.extract(), other.extract());
    var expiry = result
        // When result is true, expire when result is false, which has opposite strictness to this
        ? other.subtract(this).findSupported(t -> !comp.test(this.evaluate(t), other.evaluate(t)), !strict)
        // When result is false, expire when result is true, which has the same strictness to this
        : this.subtract(other).findSupported(t -> comp.test(this.evaluate(t), other.evaluate(t)), strict);
    return expiring(discrete(result), expiry(expiry));
  }

  public Expiring<Discrete<Boolean>> greaterThan(Polynomial other) {
    return greaterThan(other, true);
  }

  public Expiring<Discrete<Boolean>> greaterThanOrEquals(Polynomial other) {
    return greaterThan(other, false);
  }

  public Expiring<Discrete<Boolean>> lessThan(Polynomial other) {
    return other.greaterThan(this);
  }

  public Expiring<Discrete<Boolean>> lessThanOrEquals(Polynomial other) {
    return other.greaterThanOrEquals(this);
  }

  private boolean dominates$(Polynomial other) {
    for (int i = 0; i <= Math.max(this.degree(), other.degree()); ++i) {
      if (this.getCoefficient(i) > other.getCoefficient(i)) return true;
      if (this.getCoefficient(i) < other.getCoefficient(i)) return false;
    }
    // Equal, so either answer is correct
    return true;
  }

  private Expiring<Discrete<Boolean>> dominates(Polynomial other) {
    boolean result = this.dominates$(other);
    var difference = this.subtract(other);
    var expiry = difference.isConstant() ? NEVER : expiry(result
        // When result is true, this dominates other, change when this doesn't dominate.
        ? other.subtract(this).findSupported(t -> !this.step(t).dominates$(other.step(t)), false)
        // When result is false, other dominates this, change when this does dominate.
        : difference.findSupported(t -> this.step(t).dominates$(other.step(t)), false));
    return expiring(discrete(result), expiry);
  }

  public Expiring<Polynomial> min(Polynomial other) {
    return ExpiringMonad.map(this.dominates(other), d -> d.extract() ? other : this);
  }

  public Expiring<Polynomial> max(Polynomial other) {
    return ExpiringMonad.map(this.dominates(other), d -> d.extract() ? this : other);
  }

  /**
   * Finds all roots of this function in the future
   */
  private Stream<Duration> findFutureRoots() {
    // If this polynomial can never have a root, fail immediately
    if (this.isNonFinite() || this.isConstant()) {
      return Stream.empty();
    }

    final Complex[] solutions = new LaguerreSolver().solveAllComplex(coefficients, 0);
    return Arrays.stream(solutions)
                 .filter(solution -> Math.abs(solution.getImaginary()) < ROOT_FINDING_IMAGINARY_COMPONENT_TOLERANCE)
                 .map(Complex::getReal)
                 .filter(t -> t >= 0 && t <= MAX_SECONDS_FOR_DURATION)
                 .sorted()
                 .map(t -> Duration.roundNearest(t, SECOND));
  }
  private static final double MAX_SECONDS_FOR_DURATION = Duration.MAX_VALUE.ratioOver(SECOND);

  /**
   * Get the nth coefficient.
   * @param n the n.
   * @return the nth coefficient
   */
  public double getCoefficient(int n) {
    return n >= coefficients().length ? 0.0 : coefficients()[n];
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Polynomial that = (Polynomial) o;
    return Arrays.equals(coefficients, that.coefficients);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(coefficients);
  }

  @Override
  public String toString() {
    return "Polynomial{" +
           "coefficients=" + Arrays.toString(coefficients) +
           '}';
  }
}
