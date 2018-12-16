package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.EvidenceModuleDependency;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.*;

public class JungEvidenceModuleGraph implements ModuleGraph<EvidenceModuleDependency> {

  private final DirectedGraph<HWModule, TrackingModuleDependency> graph;

  private final Optional<Integer> evidenceLimit;


  public JungEvidenceModuleGraph(DirectedGraph<HWModule, TrackingModuleDependency> graph, Optional<Integer> evidenceLimit) {
    this.graph = graph;
    this.evidenceLimit = evidenceLimit;
  }


  @Override
  public Optional<EvidenceModuleDependency> findDependency(HWModule vertex1, HWModule vertex2) {
    return Optional.ofNullable(graph.findEdge(vertex1, vertex2)).map((e) ->
        new EvidenceModuleDependency(e.source, e.dest, new ArrayList<>(e.getSources()).get(0), new ArrayList<>(e.getDestinations()).get(0))
    );
  }

  @Override
  public void addDependency(EvidenceModuleDependency dependency) {
    if (graph.getVertices().containsAll(Arrays.asList(dependency.destModule, dependency.sourceModule))) {
      final Optional<TrackingModuleDependency> dependencyOptional = Optional.ofNullable(graph.findEdge(dependency.sourceModule, dependency.destModule));
      final TrackingModuleDependency dep = dependencyOptional.orElseGet(() -> {
        TrackingModuleDependency newDep = new TrackingModuleDependency(dependency.sourceModule, dependency.destModule);
        graph.addEdge(newDep, dependency.sourceModule, dependency.destModule);
        return newDep;
      });
      if(evidenceLimit.map(limit -> dep.getEvidenceCounter() < limit).orElse(true)) {
        dep.addEvidence(dependency.source, dependency.dest);
      }
    }
  }

  @Override
  public void addModule(HWModule vertex) {
    graph.addVertex(vertex);
  }

  @Override
  public Collection<HWModule> dependencies(HWModule vertex) {
    return Optional.ofNullable(graph.getSuccessors(vertex)).orElse(Collections.emptyList());
  }
}
