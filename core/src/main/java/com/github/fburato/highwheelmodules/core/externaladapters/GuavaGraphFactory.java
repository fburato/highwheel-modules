package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.*;
import com.google.common.graph.NetworkBuilder;

import java.util.Optional;

public class GuavaGraphFactory implements ModuleGraphFactory {
    @Override
    public MetricModuleGraph<ModuleDependency> buildMetricModuleGraph() {
        return new GuavaModuleGraph(NetworkBuilder.directed().allowsSelfLoops(true).build());
    }

    @Override
    public GuavaEvidenceModuleGraph buildEvidenceModuleGraph(ModuleGraph<TrackingModuleDependency> trackingGraph,
            Optional<Integer> evidenceLimit) {
        return new GuavaEvidenceModuleGraph(trackingGraph, evidenceLimit);
    }

    @Override
    public GuavaTrackingModuleGraph buildTrackingModuleGraph() {
        return new GuavaTrackingModuleGraph(NetworkBuilder.directed().allowsSelfLoops(true).build());
    }
}
