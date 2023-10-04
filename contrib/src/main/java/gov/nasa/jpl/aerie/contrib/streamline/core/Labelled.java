package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;

/**
 * Attaches name and context to a datum.
 *
 * <p>
 *   Names are given explicitly, while contexts are usually tracked through an ambient context resource.
 *   Ambient context is tracked using {@link Labelled#inContext}.
 * </p>
 */
public record Labelled<V>(V data, String name, Context context) {
  private static CellResource<Discrete<Context>> ambientContext = cellResource(discrete(new Context("-", List.of())));

  public static void inContext(String name, Runnable action) {
    inContext(name, () -> {
      action.run();
      return UNIT;
    });
  }

  public static <R> R inContext(String name, Supplier<R> action) {
    ambientContext.emit(labelled("Adding context " + name, effect(c -> new Context(name, List.of(c)))));
    var result = action.get();
    ambientContext.emit(labelled("Removing context " + name, effect(c -> {
      assert c.parentContexts().size() == 1;
      return c.parentContexts().get(0);
    })));
    return result;
  }

  public static <V> Labelled<V> labelled(String name, V data) {
    return new Labelled<>(data, name, currentValue(ambientContext));
  }

  public record Context(String name, List<Context> parentContexts) {}
}
