package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.core.model.EvidenceModuleDependency;
import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.ModuleGraph;
import com.github.fburato.highwheelmodules.core.model.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.*;

public class JungTrackingModuleGraph implements ModuleGraph<EvidenceModuleDependency> {

  private final DirectedGraph<Module, TrackingModuleDependency> graph;

  private final Optional<Integer> evidenceLimit;


  public JungTrackingModuleGraph(DirectedGraph<Module, TrackingModuleDependency> graph, Optional<Integer> evidenceLimit) {
    this.graph = graph;
    this.evidenceLimit = evidenceLimit;
  }


  @Override
  public Optional<EvidenceModuleDependency> findDependency(Module vertex1, Module vertex2) {
    return Optional.ofNullable(graph.findEdge(vertex1, vertex2)).map((e) ->
        new EvidenceModuleDependency(e.source, e.dest, new ArrayList<>(e.getSources()).get(0), new ArrayList<>(e.getDestinations()).get(0))
    );
  }

  @Override
  public void addDependency(EvidenceModuleDependency dependency) {
    if (graph.getVertices().containsAll(Arrays.asList(dependency.destModule, dependency.sourceModule))) {
      final Optional<TrackingModuleDependency> dependencyOptional = Optional.ofNullable(graph.findEdge(dependency.sourceModule, dependency.destModule));
      final TrackingModuleDependency dep = dependencyOptional.orElseGet(() -> {
        TrackingModuleDependency newDep = new TrackingModuleDependency(dependency.sourceModule, dependency.destModule, evidenceLimit);
        graph.addEdge(newDep, dependency.sourceModule, dependency.destModule);
        return newDep;
      });
      dep.addEvidence(dependency.source, dependency.dest);
    }
  }

  @Override
  public void addModule(Module vertex) {
    graph.addVertex(vertex);
  }

  @Override
  public Collection<Module> dependencies(Module vertex) {
    return Optional.ofNullable(graph.getSuccessors(vertex)).orElse(Collections.emptyList());
  }
}
