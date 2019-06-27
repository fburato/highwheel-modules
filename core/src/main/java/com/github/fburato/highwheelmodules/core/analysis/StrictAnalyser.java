package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.github.fburato.highwheelmodules.core.analysis.AnalysisUtils.*;

public class StrictAnalyser {

    public static AnalyserModel.AnalysisResult analyseStrict(Definition definition, AnalysisState analysisState) {
        final ModuleGraphTransitiveClosure specTransitiveClosure = new ModuleGraphTransitiveClosure(
                analysisState.specGraph, append(definition.modules, analysisState.other));
        final ModuleGraphTransitiveClosure actualTransitiveClosure = new ModuleGraphTransitiveClosure(
                analysisState.actualGraph, append(definition.modules, analysisState.other));
        final List<AnalyserModel.EvidenceBackedViolation> dependencyViolations = getDependenciesViolations(
                specTransitiveClosure.diffPath(actualTransitiveClosure).get(), analysisState.other,
                analysisState.actualTrackingGraph);
        final List<AnalyserModel.ModuleConnectionViolation> noStrictDependencyViolations = getNoStrictDependencyViolations(
                actualTransitiveClosure, definition.noStrictDependencies, analysisState.other);
        final List<AnalyserModel.Metrics> metrics = getMetrics(analysisState.actualGraph, definition.modules,
                analysisState.actualGraph, analysisState.other);

        return new AnalyserModel.AnalysisResult(dependencyViolations, noStrictDependencyViolations, metrics);
    }

    private static List<AnalyserModel.EvidenceBackedViolation> getDependenciesViolations(
            List<ModuleGraphTransitiveClosure.PathDifference> differences, HWModule other,
            ModuleGraph<TrackingModuleDependency> trackingGraph) {
        final List<AnalyserModel.EvidenceBackedViolation> evidenceBackedViolations = new ArrayList<>(
                differences.size());
        for (ModuleGraphTransitiveClosure.PathDifference difference : differences) {
            if (!difference.source.equals(other) && !difference.dest.equals(other)) {
                evidenceBackedViolations.add(new AnalyserModel.EvidenceBackedViolation(difference.source.name,
                        difference.dest.name, getNames(difference.firstPath), getNames(difference.secondPath),
                        getEvidence(trackingGraph, difference.source, difference.secondPath)));
            }
        }
        return evidenceBackedViolations;
    }

    private static List<AnalyserModel.ModuleConnectionViolation> getNoStrictDependencyViolations(
            ModuleGraphTransitiveClosure transitiveClosure, Collection<NoStrictDependency> rules, HWModule other) {
        final List<AnalyserModel.ModuleConnectionViolation> noStrictDependencyViolations = new ArrayList<>();
        for (NoStrictDependency rule : rules) {
            if (!rule.source.equals(other) && !rule.dest.equals(other)
                    && transitiveClosure.minimumDistance(rule.source, rule.dest).get() == 1) {
                noStrictDependencyViolations
                        .add(new AnalyserModel.ModuleConnectionViolation(rule.source.name, rule.dest.name));
            }
        }
        return noStrictDependencyViolations;
    }
}
