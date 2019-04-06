package com.github.fburato.highwheelmodules.model.modules;

import java.util.Optional;

public interface ModuleGraphFactory {
    MetricModuleGraph<ModuleDependency> buildMetricModuleGraph();

    ModuleGraph<EvidenceModuleDependency> buildEvidenceModuleGraph(ModuleGraph<TrackingModuleDependency> trackingGraph,
            Optional<Integer> evidenceLimit);

    ModuleGraph<TrackingModuleDependency> buildTrackingModuleGraph();
}
