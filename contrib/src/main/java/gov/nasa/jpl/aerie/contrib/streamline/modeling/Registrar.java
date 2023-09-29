package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Reactions;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.dynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.unit;
import static java.util.function.Function.identity;

public class Registrar {
  private final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar;
  private boolean trace = false;
  private final CellResource<Discrete<Set<Exception>>> errors;

  public Registrar(final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar) {
    Resources.init();
    this.baseRegistrar = baseRegistrar;
    errors = cellResource(Discrete.discrete(Set.of()));
    var errorString = map(errors, errors$ -> errors$.stream().map(Throwable::getMessage).collect(Collectors.joining("\n")));
    discrete("errors", errorString, new StringValueMapper());
    discrete("numberOfErrors", map(errors, Set::size), new IntegerValueMapper());
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

  private <D extends Dynamics<?, D>> void logErrors(Resource<D> resource) {
    Reactions.wheneverDynamicsChange(resource, ec -> ec.match($ -> null, this::logError));
  }

  private Exception logError(Exception e) {
    errors.emit(effect(l -> {
      var l$ = new HashSet<>(l);
      l$.add(e);
      return l$;
    }));
    return e;
  }
}
