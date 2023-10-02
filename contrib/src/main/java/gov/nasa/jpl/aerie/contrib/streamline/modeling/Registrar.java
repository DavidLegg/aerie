package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.dynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.not;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.call;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.replaying;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static java.util.stream.Collectors.joining;

public class Registrar {
  private final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar;
  private boolean trace = false;
  private final CellResource<Discrete<Set<Throwable>>> errors;

  public Registrar(final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar) {
    Resources.init();
    this.baseRegistrar = baseRegistrar;
    errors = cellResource(Discrete.discrete(Set.of()));
    var errorString = map(errors, errors$ -> errors$.stream().map(Registrar::formatError).collect(joining("\n")));
    discrete("errors", errorString, new StringValueMapper());
    discrete("numberOfErrors", map(errors, Set::size), new IntegerValueMapper());
  }

  private static String formatError(Throwable e) {
    return "%s: %s".formatted(e.getClass().getSimpleName(), e.getMessage());
  }

  public void setTrace() {
    trace = true;
  }

  public void clearTrace() {
    trace = false;
  }

  public <Value> void discrete(final String name, final Resource<Discrete<Value>> resource, final ValueMapper<Value> mapper) {
    var registeredResource = trace ? trace(name, resource) : resource;
    baseRegistrar.discrete(
        name,
        () -> registeredResource.getDynamics().match(v -> v.data().extract(), e -> null),
        new NullableValueMapper<>(mapper));
    logErrors(registeredResource);
  }

  public void real(final String name, final Resource<Linear> resource) {
    var registeredResource = trace ? trace(name, resource) : resource;
    baseRegistrar.real(name, () -> registeredResource.getDynamics().match(
        v -> RealDynamics.linear(v.data().extract(), v.data().rate()),
        e -> RealDynamics.constant(0)));
    logErrors(registeredResource);
  }

  public void assertion(final String message, final Resource<Discrete<Boolean>> assertion) {
    // Log an error when the assertion fails, i.e., on true -> false edges.
    whenever(not(assertion), () -> {
      var e = new AssertionError(message);
      logError(e);
      // Use a new task to capture the reference to e.
      // Without this, we replay and create a new object, so we can't remove the original.
      call(replaying(() -> {
        waitUntil(when(assertion));
        removeError(e);
      }));
    });
  }

  private <D extends Dynamics<?, D>> void logErrors(Resource<D> resource) {
    wheneverDynamicsChange(resource, ec -> ec.match($ -> null, this::logError));
  }

  private Unit logError(Throwable e) {
    errors.emit(effect(s -> {
      var s$ = new HashSet<>(s);
      s$.add(e);
      return s$;
    }));
    return Unit.UNIT;
  }

  private Unit removeError(Throwable e) {
    errors.emit(effect(s -> {
      var s$ = new HashSet<>(s);
      s$.remove(e);
      return s$;
    }));
    return Unit.UNIT;
  }
}
