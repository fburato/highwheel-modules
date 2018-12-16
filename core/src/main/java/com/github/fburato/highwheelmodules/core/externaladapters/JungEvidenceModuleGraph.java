package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.EvidenceModuleDependency;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;


import java.util.*;

public class JungEvidenceModuleGraph implements ModuleGraph<EvidenceModuleDependency> {

  private final ModuleGraph<TrackingModuleDependency> graph;

  private final Optional<Integer> evidenceLimit;


  public JungEvidenceModuleGraph(ModuleGraph<TrackingModuleDependency> graph, Optional<Integer> evidenceLimit) {
    this.graph = graph;
    this.evidenceLimit = evidenceLimit;
  }


  @Override
  public Optional<EvidenceModuleDependency> findDependency(HWModule vertex1, HWModule vertex2) {
    return graph.findDependency(vertex1, vertex2).map((e) ->
        new EvidenceModuleDependency(e.source, e.dest, new ArrayList<>(e.getSources()).get(0), new ArrayList<>(e.getDestinations()).get(0))
    );
  }

  @Override
  public void addDependency(EvidenceModuleDependency dependency) {
    if (graph.modules().containsAll(Arrays.asList(dependency.destModule, dependency.sourceModule))) {
      TrackingModuleDependency trackingModuleDependency = new TrackingModuleDependency(dependency.sourceModule,dependency.destModule);
      final Optional<TrackingModuleDependency> dependencyOptional = graph.findDependency(dependency.sourceModule, dependency.destModule);
      if(evidenceLimit
          .map(limit -> dependencyOptional.map(TrackingModuleDependency::getEvidenceCounter).orElse(0) < limit)
          .orElse(true)) {
        trackingModuleDependency.addEvidence(dependency.source,dependency.dest);
      }
      graph.addDependency(trackingModuleDependency);
    }
  }

  @Override
  public void addModule(HWModule vertex) {
    graph.addModule(vertex);
  }

  @Override
  public Collection<HWModule> modules() {
    return graph.modules();
  }

  @Override
  public Collection<HWModule> dependencies(HWModule vertex) {
    return graph.dependencies(vertex);
  }
}
