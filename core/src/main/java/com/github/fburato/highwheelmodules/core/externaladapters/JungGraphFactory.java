package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.*;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

import java.util.Optional;

public class JungGraphFactory implements ModuleGraphFactory {
    @Override
    public MetricModuleGraph<ModuleDependency> buildMetricModuleGraph() {
        return new JungModuleGraph(new DirectedSparseGraph<>());
    }

    @Override
    public ModuleGraph<EvidenceModuleDependency> buildEvidenceModuleGraph(
            ModuleGraph<TrackingModuleDependency> trackingGraph, Optional<Integer> evidenceLimit) {
        return new JungEvidenceModuleGraph(trackingGraph, evidenceLimit);
    }

    @Override
    public ModuleGraph<TrackingModuleDependency> buildTrackingModuleGraph() {
        return new JungTrackingModuleGraph(new DirectedSparseGraph<>());
    }
}
