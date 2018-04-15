package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.ModuleDependency;
import com.github.fburato.highwheelmodules.core.model.ModuleGraph;
import com.github.fburato.highwheelmodules.core.model.ModuleMetrics;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class JungModuleGraph implements ModuleGraph<ModuleDependency>, ModuleMetrics {

  private final DirectedGraph<Module, ModuleDependency> graph;

  public JungModuleGraph(DirectedGraph<Module, ModuleDependency> graph) {
    this.graph = graph;
  }

  @Override
  public Optional<ModuleDependency> findDependency(Module vertex1, Module vertex2) {
    return Optional.ofNullable(graph.findEdge(vertex1, vertex2));
  }

  @Override
  public void addDependency(final ModuleDependency dependency) {
    if (graph.getVertices().containsAll(Arrays.asList(dependency.source, dependency.dest))) {
      Optional<ModuleDependency> dependencyOptional = Optional.ofNullable(graph.findEdge(dependency.source, dependency.dest));
      final ModuleDependency moduleDependency = dependencyOptional.orElseGet(() -> {
        graph.addEdge(dependency, dependency.source, dependency.dest);
        return dependency;
      });
      moduleDependency.incrementCount();
    }
  }

  @Override
  public void addModule(Module vertex) {
    graph.addVertex(vertex);
  }

  @Override
  public Collection<Module> dependencies(Module vertex) {
    return Optional.ofNullable(graph.getSuccessors(vertex)).orElse(Collections.<Module>emptyList());
  }

  @Override
  public Optional<Integer> fanInOf(Module module) {
    if (!graph.containsVertex(module))
      return Optional.empty();
    else {
      final Optional<ModuleDependency> self = findDependency(module, module);
      return Optional.of(graph.inDegree(module) - self.map((a) -> 1).orElse(0));
    }
  }

  @Override
  public Optional<Integer> fanOutOf(Module module) {
    if (!graph.containsVertex(module))
      return Optional.empty();
    else {
      final Optional<ModuleDependency> self = findDependency(module, module);
      return Optional.of(graph.outDegree(module) - self.map((a) -> 1).orElse(0));
    }
  }
}
