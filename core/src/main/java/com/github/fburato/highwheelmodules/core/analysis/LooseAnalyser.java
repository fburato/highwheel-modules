package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.fburato.highwheelmodules.core.analysis.AnalysisUtils.*;

public class LooseAnalyser implements Analyser {

    @Override
    public AnalyserModel.AnalysisResult analyse(AnalysisState state) {
        return analyseLoose(state);
    }

    private AnalyserModel.AnalysisResult analyseLoose(AnalysisState analysisState) {
        final ModuleGraphTransitiveClosure actualTransitiveClosure = new ModuleGraphTransitiveClosure(
                analysisState.actualGraph, append(analysisState.modules, analysisState.other));

        final List<AnalyserModel.ModuleConnectionViolation> absentDependencyViolations = getAbsentDependencies(
                actualTransitiveClosure, analysisState.dependencies, analysisState.other);
        final List<AnalyserModel.EvidenceBackedViolation> undesiredDependencyViolations = getUndesiredDependecies(
                actualTransitiveClosure, analysisState.noStrictDependencies, analysisState.other,
                analysisState.actualTrackingGraph);

        return new AnalyserModel.AnalysisResult(undesiredDependencyViolations, absentDependencyViolations, getMetrics(
                analysisState.actualGraph, analysisState.modules, analysisState.actualGraph, analysisState.other));
    }

    private List<AnalyserModel.ModuleConnectionViolation> getAbsentDependencies(
            ModuleGraphTransitiveClosure transitiveClosure, Collection<Dependency> dependencies, HWModule other) {
        final List<AnalyserModel.ModuleConnectionViolation> dependencyViolations = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            if (!dependency.source.equals(other) && !dependency.dest.equals(other)
                    && !transitiveClosure.isReachable(dependency.source, dependency.dest)) {
                dependencyViolations
                        .add(new AnalyserModel.ModuleConnectionViolation(dependency.source.name, dependency.dest.name));
            }
        }

        return dependencyViolations;
    }

    private List<AnalyserModel.EvidenceBackedViolation> getUndesiredDependecies(
            ModuleGraphTransitiveClosure transitiveClosure, Collection<NoStrictDependency> noStrictDependencies,
            HWModule other, ModuleGraph<TrackingModuleDependency> trackingGraph) {
        final List<AnalyserModel.EvidenceBackedViolation> undesiredDependencyViolations = new ArrayList<>();
        for (NoStrictDependency noStrictDependency : noStrictDependencies) {
            if (!noStrictDependency.source.equals(other) && !noStrictDependency.dest.equals(other)
                    && transitiveClosure.isReachable(noStrictDependency.source, noStrictDependency.dest)) {
                undesiredDependencyViolations.add(new AnalyserModel.EvidenceBackedViolation(
                        noStrictDependency.source.name, noStrictDependency.dest.name,
                        Arrays.asList(noStrictDependency.source.name, noStrictDependency.dest.name),
                        getNames(transitiveClosure.minimumDistancePath(noStrictDependency.source,
                                noStrictDependency.dest)),
                        getEvidence(trackingGraph, noStrictDependency.source, transitiveClosure
                                .minimumDistancePath(noStrictDependency.source, noStrictDependency.dest))));
            }
        }
        return undesiredDependencyViolations;
    }
}
