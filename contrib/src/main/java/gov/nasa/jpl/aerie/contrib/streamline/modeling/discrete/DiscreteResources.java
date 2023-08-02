package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteMonad.map;

public final class DiscreteResources {
  private DiscreteResources() {}

  public static Condition when(Resource<Discrete<Boolean>> resource) {
    return (positive, atEarliest, atLatest) ->
        Optional.of(atEarliest).filter(t -> currentValue(resource) == positive);
  }

  public static <V> Condition when(Resource<Discrete<V>> resource, Predicate<V> predicate) {
      return when(DiscreteResourceMonad.map(resource, predicate::test));
  }

  public static <V> Resource<Discrete<V>> cache(Resource<Discrete<V>> resource, BiPredicate<V, V> updatePredicate) {
    final var cell = allocate(resource.getDynamics());
    final Resource<Discrete<V>> result = () -> cell.get().dynamics;
    // TODO: Use more efficient repeating task
    spawn(() -> {
      while (true) {
        final V currentValue = currentValue(result);
        waitUntil(when(resource, newValue -> updatePredicate.test(currentValue, newValue)));
        cell.emit(x -> resource.getDynamics());
      }
    });

    return result;
  }

  public static UnitAware<Resource<Discrete<Double>>> unitAware(Resource<Discrete<Double>> resource, Unit unit) {
    return UnitAwareResources.unitAware(resource, unit, DiscreteResources::discreteScaling);
  }

  public static UnitAware<CellResource<Discrete<Double>>> unitAware(CellResource<Discrete<Double>> resource, Unit unit) {
    return UnitAwareResources.unitAware(resource, unit, DiscreteResources::discreteScaling);
  }

  private static Discrete<Double> discreteScaling(Discrete<Double> d, Double scale) {
    return map(d, $ -> $ * scale);
  }
}
