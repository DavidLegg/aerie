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
   * @param elideAnonymousNodes When true, remove anonymous nodes and replace them with their dependencies.
   */
  public static Set<Object> getDependencies(Object dependent, boolean elideAnonymousNodes) {
    if (!elideAnonymousNodes) {
      return getDependencies(dependent);
    } else {
      Set<Object> result = new HashSet<>();
      Set<Object> visited = new HashSet<>();
      Deque<Object> frontier = new ArrayDeque<>(getDependencies(dependent));
      Object node;
      while ((node = frontier.poll()) != null) {
        if (!visited.add(node)) continue;
        if (Naming.getName(node).isEmpty()) {
          // Node is anonymous and interior, elide it by adding dependencies to frontier
          frontier.addAll(getDependencies(node));
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
   *
   * @param elideAnonymousNodes When true, remove anonymous nodes and replace them with their dependencies.
   * @param elideLeaves When true, don't print separate lines for leaf nodes
   * @param mermaidFormat When true, print output in <a href="https://mermaid.js.org/">Mermaid</a> graph syntax
   */
  public static String describeDependencyGraph(Object source, boolean elideAnonymousNodes, boolean elideLeaves, boolean mermaidFormat) {
    StringBuilder builder = new StringBuilder();
    if (mermaidFormat) {
      builder.append("flowchart TD\n");
    }
    Map<Object, String> tempNames = new HashMap<>();
    Map<Object, String> nodeIds = new HashMap<>();
    for (Object node : topologicalSort(source, elideAnonymousNodes)) {
      var dependencies = getDependencies(node, elideAnonymousNodes);
      if (dependencies.isEmpty()) {
        if (mermaidFormat) {
          builder.append("  ")
                  .append(nodeId(node, nodeIds))
                  .append("(\"")
                  .append(nodeName(node, tempNames).replace("\"", "#quot;"))
                  .append("\")\n");
        } else if (!elideLeaves || node == source) {
          builder.append(nodeName(node, tempNames)).append(" is a leaf\n");
        }
      } else {
        if (mermaidFormat) {
          builder.append("  ")
                  .append(nodeId(node, nodeIds))
                  .append("(\"")
                  .append(nodeName(node, tempNames).replace("\"", "#quot;"))
                  .append("\") --> ")
                  .append(dependencies.stream().map(n -> nodeId(n, nodeIds)).collect(joining(" & ")))
                  .append("\n");
        } else {
          builder.append(nodeName(node, tempNames))
                  .append(" --> ")
                  .append(dependencies.stream().map(n -> nodeName(n, tempNames)).collect(joining(", ")))
                  .append("\n");
        }
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

  private static String nodeId(Object node, Map<Object, String> nodeIds) {
    return nodeIds.computeIfAbsent(node, $ -> "N" + nodeIds.size());
  }
}
