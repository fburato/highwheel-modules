package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.CompoundAccessVisitor;
import com.github.fburato.highwheelmodules.core.algorithms.ModuleDependenciesGraphBuildingVisitor;
import com.github.fburato.highwheelmodules.model.modules.*;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;

import java.util.Collection;
import java.util.Optional;

public class DefinitionVisitor {

    private final ModuleGraphFactory factory;
    private final Optional<Integer> evidenceLimit;

    public DefinitionVisitor(ModuleGraphFactory factory, Optional<Integer> evidenceLimit) {
        this.factory = factory;
        this.evidenceLimit = evidenceLimit;
    }

    public AnalysisState getAnalysisState(Definition definition) {
        final HWModule other = HWModule.make("(other)", "").get();
        final Collection<HWModule> modules = definition.modules;
        if (modules.isEmpty())
            throw new AnalyserException("No modules provided in definition");
        final MetricModuleGraph<ModuleDependency> specModuleGraph = initialiseSpecificationGraph(modules,
                definition.dependencies);
        final MetricModuleGraph<ModuleDependency> actualModuleGraph = initialiseEmptyGraph();
        final ModuleGraph<TrackingModuleDependency> auxTrackingBareGraph = factory.buildTrackingModuleGraph();
        final ModuleGraph<EvidenceModuleDependency> trackingGraph = factory
                .buildEvidenceModuleGraph(auxTrackingBareGraph, evidenceLimit);
        final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<ModuleDependency> moduleGraphBuilder = (
                sourceModule, destModule, sourceAP, destAP, type) -> new ModuleDependency(sourceModule, destModule);
        final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<EvidenceModuleDependency> evidenceGraphBuilder = (
                sourceModule, destModule, sourceAP, destAP,
                type) -> new EvidenceModuleDependency(sourceModule, destModule, sourceAP, destAP);
        final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> moduleGraphVisitor = new ModuleDependenciesGraphBuildingVisitor<>(
                modules, actualModuleGraph, other, moduleGraphBuilder, definition.whitelist, definition.blackList);
        final ModuleDependenciesGraphBuildingVisitor<EvidenceModuleDependency> evidenceGraphVisitor = new ModuleDependenciesGraphBuildingVisitor<>(
                modules, trackingGraph, other, evidenceGraphBuilder, definition.whitelist, definition.blackList);
        final AccessVisitor accessVisitor = new CompoundAccessVisitor(moduleGraphVisitor, evidenceGraphVisitor);
        return new AnalysisState(definition.modules, definition.dependencies, definition.noStrictDependencies,
                specModuleGraph, actualModuleGraph, auxTrackingBareGraph, accessVisitor, other);
    }

    private MetricModuleGraph<ModuleDependency> initialiseSpecificationGraph(Collection<HWModule> modules,
            Collection<Dependency> dependencies) {
        final MetricModuleGraph<ModuleDependency> specModuleGraph = factory.buildMetricModuleGraph();

        for (HWModule module : modules) {
            specModuleGraph.addModule(module);
        }
        for (Dependency dep : dependencies) {
            specModuleGraph.addDependency(new ModuleDependency(dep.source(), dep.dest()));
        }

        return specModuleGraph;
    }

    private MetricModuleGraph<ModuleDependency> initialiseEmptyGraph() {
        return factory.buildMetricModuleGraph();
    }
}
