package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.github.fburato.highwheelmodules.core.analysis.AnalysisUtils.*;

public class LooseAnalyser {

    public static AnalyserModel.LooseAnalysisResult analyseLoose(Definition definition, AnalysisState analysisState) {
        final ModuleGraphTransitiveClosure actualTransitiveClosure = new ModuleGraphTransitiveClosure(
                analysisState.actualGraph, append(definition.modules, analysisState.other));

        final List<AnalyserModel.AbsentDependencyViolation> absentDependencyViolations = getAbsentDependencies(
                actualTransitiveClosure, definition.dependencies, analysisState.other);
        final List<AnalyserModel.UndesiredDependencyViolation> undesiredDependencyViolations = getUndesiredDependecies(
                actualTransitiveClosure, definition.noStrictDependencies, analysisState.other,
                analysisState.actualTrackingGraph);

        return new AnalyserModel.LooseAnalysisResult(absentDependencyViolations, undesiredDependencyViolations,
                getMetrics(analysisState.actualGraph, definition.modules, analysisState.actualGraph,
                        analysisState.other));
    }

    private static List<AnalyserModel.AbsentDependencyViolation> getAbsentDependencies(
            ModuleGraphTransitiveClosure transitiveClosure, Collection<Dependency> dependencies, HWModule other) {
        final List<AnalyserModel.AbsentDependencyViolation> dependencyViolations = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            if (!dependency.source.equals(other) && !dependency.dest.equals(other)
                    && !transitiveClosure.isReachable(dependency.source, dependency.dest)) {
                dependencyViolations
                        .add(new AnalyserModel.AbsentDependencyViolation(dependency.source.name, dependency.dest.name));
            }
        }

        return dependencyViolations;
    }

    private static List<AnalyserModel.UndesiredDependencyViolation> getUndesiredDependecies(
            ModuleGraphTransitiveClosure transitiveClosure, Collection<NoStrictDependency> noStrictDependencies,
            HWModule other, ModuleGraph<TrackingModuleDependency> trackingGraph) {
        final List<AnalyserModel.UndesiredDependencyViolation> undesiredDependencyViolations = new ArrayList<>();
        for (NoStrictDependency noStrictDependency : noStrictDependencies) {
            if (!noStrictDependency.source.equals(other) && !noStrictDependency.dest.equals(other)
                    && transitiveClosure.isReachable(noStrictDependency.source, noStrictDependency.dest)) {
                undesiredDependencyViolations.add(new AnalyserModel.UndesiredDependencyViolation(
                        noStrictDependency.source.name, noStrictDependency.dest.name,
                        getNames(transitiveClosure.minimumDistancePath(noStrictDependency.source,
                                noStrictDependency.dest)),
                        getEvidence(trackingGraph, noStrictDependency.source, transitiveClosure
                                .minimumDistancePath(noStrictDependency.source, noStrictDependency.dest))));
            }
        }
        return undesiredDependencyViolations;
    }
}
