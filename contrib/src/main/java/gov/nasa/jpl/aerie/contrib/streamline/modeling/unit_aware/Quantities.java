package gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware.unitAware;

public final class Quantities {
  public static UnitAware<Double> quantity(double amount) {
    return quantity(amount, Unit.SCALAR);
  }

  public static UnitAware<Double> quantity(double amount, Unit unit) {
    return unitAware(amount, unit, Quantities::scaling);
  }

  public static UnitAware<Double> quantity(Duration duration) {
    return unitAware(duration.ratioOver(Duration.SECOND), StandardUnits.SECOND, Quantities::scaling);
  }

  public static UnitAware<Double> add(UnitAware<Double> p, UnitAware<Double> q) {
    return UnitAwareOperations.add(Quantities::scaling, p, q, (x, y) -> x + y);
  }

  public static UnitAware<Double> subtract(UnitAware<Double> p, UnitAware<Double> q) {
    return UnitAwareOperations.subtract(Quantities::scaling, p, q, (x, y) -> x - y);
  }

  public static UnitAware<Double> multiply(UnitAware<Double> p, UnitAware<Double> q) {
    return UnitAwareOperations.multiply(Quantities::scaling, p, q, (x, y) -> x * y);
  }

  public static UnitAware<Double> divide(UnitAware<Double> p, UnitAware<Double> q) {
    return UnitAwareOperations.divide(Quantities::scaling, p, q, (x, y) -> x / y);
  }

  public static UnitAware<Double> abs(UnitAware<Double> p) {
    return p.map(Math::abs);
  }

  public static boolean lessThan(UnitAware<Double> p, UnitAware<Double> q) {
    return p.value() < q.value(p.unit());
  }

  public static boolean lessThanOrEquals(UnitAware<Double> p, UnitAware<Double> q) {
    return p.value() <= q.value(p.unit());
  }

  public static boolean greaterThan(UnitAware<Double> p, UnitAware<Double> q) {
    return p.value() > q.value(p.unit());
  }

  public static boolean greaterThanOrEquals(UnitAware<Double> p, UnitAware<Double> q) {
    return p.value() >= q.value(p.unit());
  }

  private static double scaling(double x, double y) {
    return x * y;
  }
}
