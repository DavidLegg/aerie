package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

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
   * Build a string formatting the dependency graph starting from source.
   */
  public static String describeDependencyGraph(Object source) {
    StringBuilder builder = new StringBuilder();
    for (Object node : topologicalSort(source)) {
      builder.append("%s --> %s%n".formatted(
          node, getDependencies(node).stream()
                                     .map(d -> Naming.getName(d, d.toString()))
                                     .collect(joining(", "))));
    }
    return builder.toString();
  }

  private static List<Object> topologicalSort(Object root) {
    // Algorithm from https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
    List<Object> result = new LinkedList<>();
    Set<Object> finished = new HashSet<>();
    Set<Object> visited = new HashSet<>();
    tsVisit(root, result, finished, visited);
    return result;
  }

  private static void tsVisit(Object node, List<Object> result, Set<Object> finished, Set<Object> visited) {
    if (finished.contains(node)) return;
    if (visited.contains(node)) return;
    visited.add(node);
    for (var dependency : getDependencies(node)) {
      tsVisit(dependency, result, finished, visited);
    }
    visited.remove(node);
    finished.add(node);
    result.add(0, node);
  }
}
