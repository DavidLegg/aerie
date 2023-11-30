package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.util.*;

import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.joining;

public final class Dependencies {
  private Dependencies() {}

  // Use a WeakHashMap so that describing a thing's dependencies
  // doesn't prevent it from being garbage-collected.
  private static final WeakHashMap<Object, Set<Object>> DEPENDENCIES = new WeakHashMap<>();
  private static final WeakHashMap<Object, Set<Object>> DEPENDENTS = new WeakHashMap<>();
  private static final String ANONYMOUS_NAME = "...";

  /**
   * Register that dependent depends on dependency.
   */
  public static void addDependency(Object dependent, Object dependency) {
    // Use WeakSet = newSetFromMap + WeakHashMap, to only weakly reference dependencies.
    DEPENDENCIES.computeIfAbsent(dependent, $ -> newSetFromMap(new WeakHashMap<>())).add(dependency);
    DEPENDENTS.computeIfAbsent(dependency, $ -> newSetFromMap(new WeakHashMap<>())).add(dependent);
  }

  /**
   * Get all registered dependencies of dependent.
   */
  public static Set<Object> getDependencies(Object dependent) {
    return DEPENDENCIES.getOrDefault(dependent, Set.of());
  }

  /**
   * Get all registered dependents of dependency.
   */
  public static Set<Object> getDependents(Object dependency) {
    return DEPENDENTS.getOrDefault(dependency, Set.of());
  }

  /**
   * Build a string formatting the dependency graph starting from source.
   * <p>
   *     The result is in <a href="https://mermaid.js.org/">Mermaid</a> syntax
   * </p>
   *
   * @param elideAnonymousNodes When true, remove anonymous nodes and replace them with their dependencies.
   */
  public static String describeDependencyGraph(Object source, boolean elideAnonymousNodes) {
    return describeDependencyGraph(List.of(source), elideAnonymousNodes);
  }

  /**
   * Build a string formatting the entire dependency graph.
   * <p>
   *     The result is in <a href="https://mermaid.js.org/">Mermaid</a> syntax
   * </p>
   *
   * @param elideAnonymousNodes When true, remove anonymous nodes and replace them with their dependencies.
   */
  public static String describeDependencyGraph(boolean elideAnonymousNodes) {
    return describeDependencyGraph(DEPENDENCIES.keySet(), elideAnonymousNodes);
  }

  /**
   * Build a string formatting the dependency graph starting from sources.
   * <p>
   *     The result is in <a href="https://mermaid.js.org/">Mermaid</a> syntax
   * </p>
   *
   * @param elideAnonymousNodes When true, remove anonymous nodes and replace them with their dependencies.
   */
  public static String describeDependencyGraph(Collection<?> sources, boolean elideAnonymousNodes) {
    Map<Object, Set<Object>> dependencyGraph = new HashMap<>();
    Map<Object, Set<Object>> dependentGraph = new HashMap<>();
    for (var source : sources) {
      buildClosure(source, dependencyGraph, dependentGraph);
    }

    if (elideAnonymousNodes) {
      // Collapse anonymous nodes out of the graph
      for (Object node : new ArrayList<>(dependencyGraph.keySet())) {
        if (nodeName(node).equals(ANONYMOUS_NAME)) {
          var dependencies = dependencyGraph.remove(node);
          var dependents = dependentGraph.remove(node);
          for (Object dependent : dependents) {
            dependencyGraph.get(dependent).remove(node);
            dependencyGraph.get(dependent).addAll(dependencies);
          }
          for (Object dependency : dependencies) {
            dependentGraph.get(dependency).remove(node);
            dependentGraph.get(dependency).addAll(dependents);
          }
        }
      }
    }

    // Describe the result
    Map<Object, String> nodeIds = new HashMap<>();
    StringBuilder builder = new StringBuilder();
    builder.append("flowchart TD\n");
    // Traverse the condensed graph in topological order
    Deque<Object> frontier = new ArrayDeque<>(dependencyGraph
            .keySet()
            .stream()
            .filter(node -> dependentGraph.getOrDefault(node, Set.of()).isEmpty())
            .toList());
    Object node;
    while ((node = frontier.poll()) != null) {
      // Handle this node:
      var dependencies = dependencyGraph.get(node);
      builder.append("  ")
              .append(nodeId(node, nodeIds))
              .append("(\"")
              .append(scrub(nodeName(node)))
              .append("\")");
      if (!dependencies.isEmpty()) {
        builder.append(" --> ")
                .append(dependencies.stream().map(n -> nodeId(n, nodeIds)).collect(joining(" & ")));
      }
      builder.append("\n");

      // Remove node from the graph and search for new frontier nodes
      dependencyGraph.remove(node);
      for (var dependency : dependencies) {
        dependentGraph.get(dependency).remove(node);
        if (dependentGraph.get(dependency).isEmpty()) {
          frontier.add(dependency);
        }
      }
    }
    return builder.toString();
  }

  private static void buildClosure(Object node, Map<Object, Set<Object>> dependencyGraph, Map<Object, Set<Object>> dependentGraph) {
    if (dependencyGraph.containsKey(node)) return;
    var dependencies = getDependencies(node);
    dependencyGraph.computeIfAbsent(node, $ -> new HashSet<>()).addAll(dependencies);
    dependentGraph.computeIfAbsent(node, $ -> new HashSet<>());
    for (var dependency : dependencies) {
      dependentGraph.computeIfAbsent(dependency, $ -> new HashSet<>()).add(node);
    }
    for (var dependency : dependencies) {
      buildClosure(dependency, dependencyGraph, dependentGraph);
    }
  }

  private static String nodeName(Object node) {
    return Naming.getName(node, ANONYMOUS_NAME);
  }

  private static String nodeId(Object node, Map<Object, String> nodeIds) {
    return nodeIds.computeIfAbsent(node, $ -> "N" + nodeIds.size());
  }

  private static String scrub(String label) {
    // Subset of commonly-used characters and their html entity replacements.
    // An HTML escaping method would be better, but it's not worth adding a library just for that.
    return label
            .replace("\"", "&quot;")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
  }
}
