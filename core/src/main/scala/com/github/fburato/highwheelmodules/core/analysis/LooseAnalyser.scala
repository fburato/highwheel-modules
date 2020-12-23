package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure
import com.github.fburato.highwheelmodules.model.modules.{HWModuleS, ModuleGraphS, TrackingModuleDependencyS}
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}

private[analysis] object LooseAnalyser extends Analyser {
  override def analyse(state: AnalysisState): AnalysisResult = {
    val actualTransitiveClosure = ModuleGraphTransitiveClosure(state.actualGraph, (state.modules ++ Seq(state.other)))
    val absentDependencies = getAbsentDependencies(actualTransitiveClosure, state.dependencies, state.other)
    val undesiredDependencies = getUndesiredDependencies(actualTransitiveClosure, state.noStrictDependencies, state.other, state.actualTrackingGraph)

    AnalysisResult(undesiredDependencies,
      absentDependencies, getMetrics(state.actualGraph, state.modules, state.actualGraph, state.other))
  }

  private def getAbsentDependencies(transitiveClosure: ModuleGraphTransitiveClosure,
                                    dependencies: Seq[DependencyS],
                                    other: HWModuleS): Seq[ModuleConnectionViolation] = {
    dependencies
      .filter(d => d.source != other && d.dest != other && !transitiveClosure.isReachable(d.source, d.dest))
      .map(d => ModuleConnectionViolation(d.source.name, d.dest.name))
  }

  private def getUndesiredDependencies(transitiveClosure: ModuleGraphTransitiveClosure,
                                       noStrictDependencies: Seq[NoStrictDependencyS],
                                       other: HWModuleS,
                                       trackingGraph: ModuleGraphS[TrackingModuleDependencyS]): Seq[EvidenceBackedViolation] = {
    noStrictDependencies
      .filter(d => d.source != other && d.dest != other && transitiveClosure.isReachable(d.source, d.dest))
      .map(d => EvidenceBackedViolation(d.source.name, d.dest.name,
        List(d.source.name, d.dest.name),
        getNames(transitiveClosure.minimumDistancePath(d.source, d.dest)),
        getEvidence(trackingGraph, d.source, transitiveClosure.minimumDistancePath(d.source, d.dest))))
  }
}
