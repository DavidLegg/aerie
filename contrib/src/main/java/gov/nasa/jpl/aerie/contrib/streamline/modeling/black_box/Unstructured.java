package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.function.TriFunction;

import java.sql.Time;
import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * Dynamics with no observable structure.
 * While very general, these need to be approximated by more structured
 * dynamics to report out to Aerie.
 */
public sealed interface Unstructured<T> extends Dynamics<T, Unstructured<T>> {
  static <T> Unstructured<T> timeBased(Function<Duration, T> valueOverTime) {
    return new TimeBased<>(valueOverTime, ZERO);
  }
  static <A, B> Unstructured<B> map(Dynamics<A, ?> a, Function<A, B> f) {
    return new UnaryMapped<>(a, f);
  }
  // TODO: Look into the theory of applicatives, see if that could simplify this code any
  static <A, B, C> Unstructured<C> map(Dynamics<A, ?> a, Dynamics<B, ?> b, BiFunction<A, B, C> f) {
    return new BinaryMapped<>(a, b, f);
  }

  record TimeBased<T>(Function<Duration, T> valueOverTime, Duration time) implements Unstructured<T> {
    @Override
    public T extract() {
      return valueOverTime.apply(time);
    }

    @Override
    public Unstructured<T> step(final Duration t) {
      return new TimeBased<>(valueOverTime, time.plus(t));
    }

    @Override
    public boolean areEqualResults(final Unstructured<T> left, final Unstructured<T> right) {
      if (left instanceof Unstructured.TimeBased<T>) {
        // Time-based results are equal when they are equal records (same function, same time)
        return left.equals(right);
      } else {
        // We can't be of any help here, use left as baseline
        return left.areEqualResults(left, right);
      }
    }
  }

  record UnaryMapped<A, D extends Dynamics<A, D>, B>(Dynamics<A, D> a, Function<A, B> f) implements Unstructured<B> {
    @Override
    public B extract() {
      return f.apply(a.extract());
    }

    @Override
    public Unstructured<B> step(final Duration t) {
      return map(a.step(t), f);
    }

    @Override
    public boolean areEqualResults(final Unstructured<B> left, final Unstructured<B> right) {
      if (left instanceof Unstructured.UnaryMapped<?, ?, ?> leftUnary) {
        return right instanceof Unstructured.UnaryMapped<?, ?, ?> rightUnary &&
               // HACK - use step(0) to get "this" as its self type parameter
               this.a.areEqualResults((D) leftUnary.a, (D) rightUnary.a) &&
               leftUnary.f.equals(rightUnary.f);
      } else {
        // We can't be of any use here, use left as baseline
        return left.areEqualResults(left, right);
      }
    }
  }

  record BinaryMapped<A, D extends Dynamics<A, D>, B, E extends Dynamics<B, E>, C>(Dynamics<A, D> a, Dynamics<B, E> b, BiFunction<A, B, C> f) implements Unstructured<C> {
    @Override
    public C extract() {
      return f.apply(a.extract(), b.extract());
    }

    @Override
    public Unstructured<C> step(final Duration t) {
      return map(a.step(t), b.step(t), f);
    }

    @Override
    public boolean areEqualResults(final Unstructured<C> left, final Unstructured<C> right) {
      if (left instanceof Unstructured.BinaryMapped<?, ?, ?, ?, ?> leftBinary) {
        return right instanceof Unstructured.BinaryMapped<?, ?, ?, ?, ?> rightBinary &&
               // HACK - use step(0) to get "this" as its self type parameter
               this.a.areEqualResults((D) leftBinary.a, (D) rightBinary.a) &&
               this.b.areEqualResults((E) leftBinary.b, (E) rightBinary.b) &&
               leftBinary.f.equals(rightBinary.f);
      } else {
        // We can't be of any use here, use left as baseline
        return left.areEqualResults(left, right);
      }
    }
  }
}
