package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.*;
import com.google.common.graph.MutableNetwork;

import java.util.*;

public class GuavaModuleGraph implements MetricModuleGraph<ModuleDependency> {

  private final MutableNetwork<HWModule, ModuleDependency> graph;

  public GuavaModuleGraph(MutableNetwork<HWModule, ModuleDependency> graph) {
    this.graph = graph;
  }

  @Override
  public Optional<ModuleDependency> findDependency(HWModule vertex1, HWModule vertex2) {
    if(graph.nodes().containsAll(Arrays.asList(vertex1,vertex2))) {
      return graph.edgeConnecting(vertex1, vertex2);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void addDependency(final ModuleDependency dependency) {
    if (graph.nodes().containsAll(Arrays.asList(dependency.source, dependency.dest))) {
      Optional<ModuleDependency> dependencyOptional = graph.edgeConnecting(dependency.source, dependency.dest);
      final ModuleDependency moduleDependency = dependencyOptional.orElseGet(() -> {
        graph.addEdge(dependency.source, dependency.dest,dependency);
        return dependency;
      });
      moduleDependency.incrementCount();
    }
  }

  @Override
  public void addModule(HWModule vertex) {
    graph.addNode(vertex);
  }

  @Override
  public Collection<HWModule> modules() {
    return graph.nodes();
  }

  @Override
  public Collection<HWModule> dependencies(HWModule vertex) {
    if(graph.nodes().contains(vertex)) {
      return graph.successors(vertex);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Optional<Integer> fanInOf(HWModule module) {
    if (!graph.nodes().contains(module))
      return Optional.empty();
    else {
      final Optional<ModuleDependency> self = findDependency(module, module);
      return Optional.of(graph.inDegree(module) - self.map((a) -> 1).orElse(0));
    }
  }

  @Override
  public Optional<Integer> fanOutOf(HWModule module) {
    if (!graph.nodes().contains(module))
      return Optional.empty();
    else {
      final Optional<ModuleDependency> self = findDependency(module, module);
      return Optional.of(graph.outDegree(module) - self.map((a) -> 1).orElse(0));
    }
  }
}
