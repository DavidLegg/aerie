package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Condition;

import java.util.Stack;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;

/**
 * Functions for debugging resources by tracing their calculation.
 */
public final class Tracing {
  private Tracing() {}

  private final static Stack<String> activeTracePoints = new Stack<>();

  public static <D> Resource<D> trace(Resource<D> resource) {
    return trace(Naming.getName(resource, "anonymous resource"), resource);
  }

  public static <D> Resource<D> trace(String name, Resource<D> resource) {
    return () -> traceAction(name, resource::getDynamics);
  }

  public static <D extends Dynamics<?, D>> CellResource<D> trace(CellResource<D> resource) {
    return trace(Naming.getName(resource, "anonymous resource"), resource);
  }

  public static <D extends Dynamics<?, D>> CellResource<D> trace(String name, CellResource<D> resource) {
    return new CellResource<D>() {
      private final Resource<D> tracedResoure = trace((Resource<D>) resource);

      @Override
      public void emit(final DynamicsEffect<D> effect) {
        resource.emit(effect);
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return tracedResoure.getDynamics();
      }
    };
  }

  public static Condition trace(Condition condition) {
    return trace(Naming.getName(condition, "anonymous condition"), condition);
  }

  public static Condition trace(String name, Condition condition) {
    return (positive, atEarliest, atLatest) ->
        traceAction(name + " evaluate (%s, %s, %s)".formatted(positive, atEarliest, atLatest), () -> condition.nextSatisfied(positive, atEarliest, atLatest));
  }

  public static Supplier<Condition> trace(Supplier<Condition> condition) {
    return trace(Naming.getName(condition, "anonymous condition"), condition);
  }

  public static Supplier<Condition> trace(String name, Supplier<Condition> condition) {
    // Trace calling the supplier separately from tracing the condition itself.
    return () -> traceAction(name + " (generation)", () -> trace(name, condition.get()));
  }

  private static <T> T traceAction(String name, Supplier<T> action) {
    activeTracePoints.push(name);
    System.out.printf("TRACE: %s - %s start...%n", currentTime(), formatStack());
    T result = action.get();
    System.out.printf("TRACE: %s - %s: %s%n", currentTime(), formatStack(), result);
    activeTracePoints.pop();
    return result;
  }

  private static String formatStack() {
    return String.join("->", activeTracePoints);
  }
}
