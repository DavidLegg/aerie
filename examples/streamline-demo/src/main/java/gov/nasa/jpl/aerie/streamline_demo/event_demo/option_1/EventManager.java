package gov.nasa.jpl.aerie.streamline_demo.event_demo.option_1;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.value;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.add;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.remove;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;

/*
    This is an attempt to model events *very* explicitly, and have records of them that live in a resource in the simulation.
    To be frank, I'm not convinced this is a good way to go about it.
    I don't know that there's a compelling reason to separate the event from its handler, for the kind of events we're considering.
 */

public class EventManager<E> {
    private final MutableResource<Discrete<List<E>>> _activeEvents = discreteResource(List.of());
    // Publicly expose the resource, but without allowing arbitrary effects on it.
    // Note that by exposing the original resource directly, we naturally pick up names applied by consumers of this resource.
    public final Resource<Discrete<List<E>>> activeEvents = _activeEvents;
    private final Resource<Discrete<Optional<E>>> firstEvent = map(_activeEvents, q -> q.stream().findFirst());

    public void activate(E event) {
        add(_activeEvents, event);
    }

    public void resolve(E event) {
        boolean wasRemoved = remove(_activeEvents, event);
        if (!wasRemoved) {
            throw new RuntimeException("Event " + event + " was not active, so cannot be resolved.");
        }
    }

    /**
     * Add a generic event handler.
     * <p>
     *     This class assumes that at least one handler will resolve each event. The manager may stall if this isn't true.
     * </p>
     */
    public void addHandler(Consumer<E> handler) {
        wheneverDynamicsChange(firstEvent, event -> value(event, Optional.empty()).ifPresent(handler));
    }

    /**
     * Add an event handler for events satisfying this predicate.
     * <p>
     *     Automatically resolves events satisfying this predicate after running handler on those events.
     *     Handler should <em>not</em> resolve the event itself.
     * </p>
     */
    public void addHandler(Predicate<E> predicate, Consumer<E> handler) {
        addHandler(event -> {
            if (predicate.test(event)) {
                handler.accept(event);
                resolve(event);
            }
        });
    }
}
