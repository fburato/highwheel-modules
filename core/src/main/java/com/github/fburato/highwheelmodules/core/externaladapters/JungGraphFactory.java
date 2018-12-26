package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.*;
import com.google.common.graph.NetworkBuilder;

import java.util.Optional;

public class JungGraphFactory implements ModuleGraphFactory {
  @Override
  public JungModuleGraph buildMetricModuleGraph() {
    return new JungModuleGraph(NetworkBuilder.directed().allowsSelfLoops(true).build());
  }

  @Override
  public JungEvidenceModuleGraph buildEvidenceModuleGraph(ModuleGraph<TrackingModuleDependency> trackingGraph, Optional<Integer> evidenceLimit) {
    return new JungEvidenceModuleGraph(trackingGraph,evidenceLimit);
  }

  @Override
  public JungTrackingModuleGraph buildTrackingModuleGraph() {
    return new JungTrackingModuleGraph(NetworkBuilder.directed().allowsSelfLoops(true).build());
  }
}
