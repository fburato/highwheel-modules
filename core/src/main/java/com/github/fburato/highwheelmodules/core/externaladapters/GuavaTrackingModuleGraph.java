package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import com.google.common.graph.MutableNetwork;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class GuavaTrackingModuleGraph implements ModuleGraph<TrackingModuleDependency> {

  private final MutableNetwork<HWModule,TrackingModuleDependency> graph;

  public GuavaTrackingModuleGraph(MutableNetwork<HWModule,TrackingModuleDependency> graph) {
    this.graph = graph;
  }

  @Override
  public Optional<TrackingModuleDependency> findDependency(HWModule vertex1, HWModule vertex2) {
    if(graph.nodes().containsAll(Arrays.asList(vertex1,vertex2))) {
      return graph.edgeConnecting(vertex1, vertex2);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void addDependency(TrackingModuleDependency dependency) {
    if(graph.nodes().containsAll(Arrays.asList(dependency.source,dependency.dest))) {
      final TrackingModuleDependency dep = graph.edgeConnecting(dependency.source,dependency.dest).orElseGet( () -> {
        final TrackingModuleDependency newDep = new TrackingModuleDependency(dependency.source,dependency.dest);
        graph.addEdge(dependency.source,dependency.dest,newDep);
        return newDep;
      });
      for(AccessPoint s: dependency.getSources()) {
        for(AccessPoint d: dependency.getDestinationsFromSource(s)) {
          dep.addEvidence(s,d);
        }
      }
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
}
