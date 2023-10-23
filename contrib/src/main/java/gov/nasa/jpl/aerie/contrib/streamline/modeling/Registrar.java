package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling.profile;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.not;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;
import static java.util.stream.Collectors.joining;

public class Registrar {
  private final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar;
  private boolean trace = false;
  private boolean profile = false;
  private final CellResource<Discrete<Map<Throwable, Set<String>>>> errors;

  public Registrar(final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar) {
    Resources.init();
    this.baseRegistrar = baseRegistrar;
    errors = cellResource(Discrete.discrete(Map.of()));
    var errorString = map(errors, errors$ -> errors$.entrySet().stream().map(entry -> formatError(entry.getKey(), entry.getValue())).collect(joining("\n\n")));
    discrete("errors", errorString, new StringValueMapper());
    discrete("numberOfErrors", map(errors, Map::size), new IntegerValueMapper());
  }

  private static String formatError(Throwable e, Collection<String> affectedResources) {
    return "Error affecting %s:%n%s".formatted(
        String.join(", ", affectedResources),
        formatException(e));
  }

  private static String formatException(Throwable e) {
    return ExceptionUtils.stream(e)
        .map(ExceptionUtils::getMessage)
        .collect(joining("\nCaused by: "));
  }

  public void setTrace() {
    trace = true;
  }

  public void clearTrace() {
    trace = false;
  }

  public void setProfile() {
    profile = true;
  }

  public void clearProfile() {
    profile = true;
  }

  public <Value> void discrete(final String name, final Resource<Discrete<Value>> resource, final ValueMapper<Value> mapper) {
    resource.registerName(name);
    var registeredResource = debug(name, resource);
    baseRegistrar.discrete(
        name,
        () -> currentValue(registeredResource, null),
        new NullableValueMapper<>(mapper));
    logErrors(name, registeredResource);
  }

  public void real(final String name, final Resource<Linear> resource) {
    resource.registerName(name);
    var registeredResource = debug(name, resource);
    baseRegistrar.real(name, () -> {
      var linear = currentData(registeredResource, linear(0, 0));
      return RealDynamics.linear(linear.extract(), linear.rate());
    });
    logErrors(name, registeredResource);
  }

  private <D> Resource<D> debug(String name, Resource<D> resource) {
    var tracedResource = trace ? trace(name, resource) : resource;
    return profile ? profile(name, tracedResource) : tracedResource;
  }

  private <D extends Dynamics<?, D>> void logErrors(String name, Resource<D> resource) {
    // TODO: Is there any way to avoid computing resources twice, once for sampling and separately for errors?
    var hasError = ErrorCatchingToResourceMonad.bind(resource, ec -> DiscreteResourceMonad.unit(ec.match(value -> false, error -> true)));
    whenever(hasError, () -> {
      resource.getDynamics().match($ -> null, e -> logError(name, e));
      // Avoid infinite loops by waiting for resource to clear before logging a new error.
      // TODO: This means we won't log if a resource changes from error1 to error2 without clearing in between.
      //   Maybe we should implement a condition like "is not the current error"?
      waitUntil(when(not(hasError)));
    });
  }

  // TODO: Consider pulling in a Guava MultiMap instead of doing this by hand below
  private Unit logError(String resourceName, Throwable e) {
    errors.emit(effect(s -> {
      var s$ = new HashMap<>(s);
      s$.compute(e, (e$, affectedResources) -> {
        if (affectedResources == null) {
          return Set.of(resourceName);
        } else {
          var affectedResources$ = new HashSet<>(affectedResources);
          affectedResources$.add(resourceName);
          return affectedResources$;
        }
      });
      return s$;
    }));
    return Unit.UNIT;
  }
}
