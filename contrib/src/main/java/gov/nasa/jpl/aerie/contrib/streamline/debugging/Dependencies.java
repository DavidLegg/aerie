package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.util.*;

import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.joining;

public final class Dependencies {
  private Dependencies() {}

  // Use a WeakHashMap so that describing a thing's dependencies
  // doesn't prevent it from being garbage-collected.
  private static final WeakHashMap<Object, Set<Object>> DEPENDENCIES = new WeakHashMap<>();

  /**
   * Register that dependent depends on dependency.
   */
  public static void addDependency(Object dependent, Object dependency) {
    // Use WeakSet = newSetFromMap + WeakHashMap, to only weakly reference dependencies.
    DEPENDENCIES.computeIfAbsent(dependent, $ -> newSetFromMap(new WeakHashMap<>())).add(dependency);
  }

  /**
   * Get all registered dependencies of dependent.
   */
  public static Set<Object> getDependencies(Object dependent) {
    return DEPENDENCIES.getOrDefault(dependent, Set.of());
  }

  /**
   * Get all registered dependencies of dependent.
   *
   * @param elideAnonymousInteriorNodes When true, remove anonymous non-leaf dependencies and replace them with their dependencies.
   */
  public static Set<Object> getDependencies(Object dependent, boolean elideAnonymousInteriorNodes) {
    if (!elideAnonymousInteriorNodes) {
      return getDependencies(dependent);
    } else {
      Set<Object> result = new HashSet<>();
      Set<Object> visited = new HashSet<>();
      Deque<Object> frontier = new ArrayDeque<>(getDependencies(dependent));
      Object node;
      while ((node = frontier.poll()) != null) {
        if (!visited.add(node)) continue;
        Set<Object> nodeDependencies;
        if (Naming.getName(node).isEmpty() && !(nodeDependencies = getDependencies(node)).isEmpty()) {
          // Node is anonymous and interior, elide it by adding dependencies to frontier
          frontier.addAll(nodeDependencies);
        } else {
          // Node is named or a leaf, include it in result and don't put dependencies on the frontier
          result.add(node);
        }
      }
      return result;
    }
  }

  /**
   * Build a string formatting the dependency graph starting from source.
   */
  public static String describeDependencyGraph(Object source, boolean elideAnonymousInteriorNodes) {
    StringBuilder builder = new StringBuilder();
    Map<Object, String> tempNames = new HashMap<>();
    for (Object node : topologicalSort(source, elideAnonymousInteriorNodes)) {
      var dependencies = getDependencies(node, elideAnonymousInteriorNodes);
      builder.append(nodeName(node, tempNames));
      if (dependencies.isEmpty()) {
        builder.append(" is a leaf\n");
      } else {
        builder.append(" --> ")
               .append(dependencies.stream().map(n -> nodeName(n, tempNames)).collect(joining(", ")))
               .append("\n");
      }
    }
    return builder.toString();
  }

  private static List<Object> topologicalSort(Object root, boolean elideAnonymousInteriorNodes) {
    // Algorithm from https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
    List<Object> result = new LinkedList<>();
    Set<Object> finished = new HashSet<>();
    Set<Object> visited = new HashSet<>();
    tsVisit(root, result, finished, visited, elideAnonymousInteriorNodes);
    return result;
  }

  private static void tsVisit(Object node, List<Object> result, Set<Object> finished, Set<Object> visited, boolean elideAnonymousInteriorNodes) {
    if (finished.contains(node)) return;
    if (visited.contains(node)) return;
    visited.add(node);
    for (var dependency : getDependencies(node, elideAnonymousInteriorNodes)) {
      tsVisit(dependency, result, finished, visited, elideAnonymousInteriorNodes);
    }
    visited.remove(node);
    finished.add(node);
    result.add(0, node);
  }

  private static String nodeName(Object node, Map<Object, String> tempNames) {
    return Naming.getName(node)
                 .orElseGet(() -> tempNames.computeIfAbsent(node, $ -> "A" + tempNames.size()));
  }
}
