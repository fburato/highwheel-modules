package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.model.modules.*;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.Collection;
import java.util.Objects;

public class AnalysisState {
    public final Collection<HWModule> modules;
    public final Collection<Dependency> dependencies;
    public final Collection<NoStrictDependency> noStrictDependencies;
    public final MetricModuleGraph<ModuleDependency> specGraph;
    public final MetricModuleGraph<ModuleDependency> actualGraph;
    public final ModuleGraph<TrackingModuleDependency> actualTrackingGraph;
    public final AccessVisitor visitor;
    public final HWModule other;

    public AnalysisState(Collection<HWModule> modules, Collection<Dependency> dependencies,
            Collection<NoStrictDependency> noStrictDependencies, MetricModuleGraph<ModuleDependency> specGraph,
            MetricModuleGraph<ModuleDependency> actualGraph, ModuleGraph<TrackingModuleDependency> actualTrackingGraph,
            AccessVisitor visitor, HWModule other) {
        this.modules = modules;
        this.dependencies = dependencies;
        this.noStrictDependencies = noStrictDependencies;
        this.specGraph = specGraph;
        this.actualGraph = actualGraph;
        this.actualTrackingGraph = actualTrackingGraph;
        this.visitor = visitor;
        this.other = other;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnalysisState that = (AnalysisState) o;
        return Objects.equals(modules, that.modules) && Objects.equals(dependencies, that.dependencies)
                && Objects.equals(noStrictDependencies, that.noStrictDependencies)
                && Objects.equals(specGraph, that.specGraph) && Objects.equals(actualGraph, that.actualGraph)
                && Objects.equals(actualTrackingGraph, that.actualTrackingGraph)
                && Objects.equals(visitor, that.visitor) && Objects.equals(other, that.other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modules, dependencies, noStrictDependencies, specGraph, actualGraph, actualTrackingGraph,
                visitor, other);
    }

    @Override
    public String toString() {
        return "AnalysisState{" + "modules=" + modules + ", dependencies=" + dependencies + ", noStrictDependencies="
                + noStrictDependencies + ", specGraph=" + specGraph + ", actualGraph=" + actualGraph
                + ", actualTrackingGraph=" + actualTrackingGraph + ", visitor=" + visitor + ", other=" + other + '}';
    }
}
