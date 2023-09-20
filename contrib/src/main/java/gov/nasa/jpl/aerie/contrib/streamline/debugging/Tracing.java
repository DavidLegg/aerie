package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Stack;
import java.util.function.Consumer;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;

/**
 * Functions for debugging resources by tracing their calculation.
 */
public final class Tracing {
  private Tracing() {}

  private final static Stack<String> activeResources = new Stack<>();

  public static <D> Resource<D> trace(String name, Resource<D> resource) {
    return traceFull(name, $ -> {}, resource);
  }

  public static <D> Resource<D> trace(String name, Consumer<D> assertion, Resource<D> resource) {
    return traceExpiring(name, $ -> assertion.accept($.data()), resource);
  }

  public static <D> Resource<D> traceExpiring(String name, Consumer<Expiring<D>> assertion, Resource<D> resource) {
    return traceFull(name, $ -> $.match(d -> {
      assertion.accept(d);
      return Unit.UNIT;
    }, e -> {
      throw new AssertionError("%s failed while computing".formatted(formatStack()), e);
    }), resource);
  }

  public static <D> Resource<D> traceFull(String name, Consumer<ErrorCatching<Expiring<D>>> assertion, Resource<D> resource) {
    return () -> {
      activeResources.push(name);
      System.out.printf("TRACE: %s - %s computing...", currentTime(), formatStack());
      var result = resource.getDynamics();
      try {
        assertion.accept(result);
      } catch (Exception e) {
        result = failure(e);
      }
      System.out.printf("TRACE: %s - %s: %s%n", currentTime(), formatStack(), result);
      activeResources.pop();
      return result;
    };
  }

  private static String formatStack() {
    return String.join("->", activeResources);
  }
}
