package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.core.algorithms.{ModuleGraphTransitiveClosure, ModuleGraphTransitiveClosureSImpl}
import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleGraph, TrackingModuleDependency}
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency

private[analysis] object StrictAnalyser extends Analyser {
  override def analyse(state: AnalysisState): AnalysisResult = {
    val specTransitiveClosure = ModuleGraphTransitiveClosure(state.specGraph, state.modules ++ Seq(state.other))
    val actualTransitiveClosure = ModuleGraphTransitiveClosure(state.actualGraph, state.modules ++ Seq(state.other))
    val dependencyViolations = getDependenciesViolations(specTransitiveClosure.diffPath(actualTransitiveClosure).head, state.other, state.actualTrackingGraph)
    val noStrictDependencyViolations = getNoStrictDependencyViolation(actualTransitiveClosure, state.noStrictDependencies, state.other)
    val metrics = getMetrics(state.actualGraph, state.modules, state.actualGraph, state.other)
    AnalysisResult(dependencyViolations, noStrictDependencyViolations, metrics)
  }

  private def getDependenciesViolations(differences: Seq[ModuleGraphTransitiveClosure.PathDifference],
                                        other: HWModule,
                                        trackingGraph: ModuleGraph[TrackingModuleDependency]): Seq[EvidenceBackedViolation] = {
    differences
      .filter(d => d.source != other && d.dest != other)
      .map(d => EvidenceBackedViolation(d.source.name, d.dest.name,
        getNames(d.firstPath),
        getNames(d.secondPath),
        getEvidence(trackingGraph, d.source, d.secondPath)))
  }

  private def getNoStrictDependencyViolation(transitiveClosure: ModuleGraphTransitiveClosureSImpl, rules: Seq[NoStrictDependency], other: HWModule): Seq[ModuleConnectionViolation] = {
    rules
      .filter(r => r.source != other && r.dest != other && transitiveClosure.minimumDistance(r.source, r.dest).head == 1)
      .map(r => ModuleConnectionViolation(r.source.name, r.dest.name))
  }
}
