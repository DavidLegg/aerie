package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Thread-local scope-bound description of the current context.
 */
public final class Context {
  private Context() {}

  private static final ThreadLocal<Deque<String>> contexts = ThreadLocal.withInitial(ArrayDeque::new);

  public static void inContext(String contextName, Runnable action) {
    // Using a thread-local context stack maintains isolation for threaded tasks.
    try {
      contexts.get().push(contextName);
      action.run();
    } finally {
      // Doing the tear-down in a finally block maintains isolation for replaying tasks.
      contexts.get().pop();
    }
  }

  /**
   * Run action in a context stack like that returned by {@link Context#get}.
   *
   * <p>
   *   This can be used to "copy" a context into another task, e.g.
   *   <pre>
   *     var context = Context.get();
   *     spawn(() -> inContext(context, () -> { ... });
   *   </pre>
   * </p>
   *
   * @see Context#contextualized
   */
  public static void inContext(List<String> contextStack, Runnable action) {
    if (contextStack.isEmpty()) {
      action.run();
    } else {
      int n = contextStack.size() - 1;
      inContext(contextStack.get(n), () ->
          inContext(contextStack.subList(0, n), action));
    }
  }

  /**
   * Adds the current context into action.
   *
   * <p>
   *   This can be used to contextualize sub-tasks with their parents context:
   *   <pre>
   *     inContext("parent", () -> {
   *       // Capture parent context while calling spawn:
   *       spawn(contextualized(() -> {
   *         // Runs child task in context "parent"
   *       }));
   *     });
   *   </pre>
   * </p>
   *
   * @see Context#contextualized(String, Runnable)
   * @see Context#inContext(List, Runnable)
   * @see Context#inContext(String, Runnable)
   */
  public static Runnable contextualized(Runnable action) {
    final var context = get();
    return () -> inContext(context, action);
  }

  /**
   * Adds the current context into action, as well as an additional child context.
   *
   * <p>
   *   This can be used to contextualize sub-tasks with their parents context:
   *   <pre>
   *     inContext("parent", () -> {
   *       // Capture parent context while calling spawn:
   *       spawn(contextualized("child", () -> {
   *         // Runs child task in context ("child", "parent")
   *       }));
   *     });
   *   </pre>
   * </p>
   *
   * @see Context#contextualized(Runnable)
   * @see Context#inContext(List, Runnable)
   * @see Context#inContext(String, Runnable)
   */
  public static Runnable contextualized(String childContext, Runnable action) {
    return contextualized(() -> inContext(childContext, action));
  }

  /**
   * Returns the list of contexts, from innermost context out.
   */
  public static List<String> get() {
    return contexts.get().stream().toList();
  }
}
