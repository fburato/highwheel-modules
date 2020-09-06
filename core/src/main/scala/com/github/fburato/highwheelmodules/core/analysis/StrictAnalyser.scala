package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure
import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure.PathDifference
import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleGraph, TrackingModuleDependency}
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency

import scala.jdk.CollectionConverters._

private[analysis] object StrictAnalyser extends Analyser {
  override def analyse(state: AnalysisState): AnalysisResult = {
    val specTransitiveClosure = new ModuleGraphTransitiveClosure(state.specGraph, (state.modules ++ Seq(state.other)).asJava)
    val actualTransitiveClosure = new ModuleGraphTransitiveClosure(state.actualGraph, (state.modules ++ Seq(state.other)).asJava)
    val dependencyViolations = getDependenciesViolations(specTransitiveClosure.diffPath(actualTransitiveClosure).get().asScala.toSeq, state.other, state.actualTrackingGraph)
    val noStrictDependencyViolations = getNoStrictDependencyViolation(actualTransitiveClosure, state.noStrictDependencies, state.other)
    val metrics = getMetrics(state.actualGraph, state.modules, state.actualGraph, state.other)
    AnalysisResult(dependencyViolations, noStrictDependencyViolations, metrics)
  }

  private def getDependenciesViolations(differences: Seq[PathDifference],
                                        other: HWModule,
                                        trackingGraph: ModuleGraph[TrackingModuleDependency]): Seq[EvidenceBackedViolation] = {
    differences
      .filter(d => d.source != other && d.dest != other)
      .map(d => EvidenceBackedViolation(d.source.name, d.dest.name,
        getNames(d.firstPath.asScala.toSeq),
        getNames(d.secondPath.asScala.toSeq),
        getEvidence(trackingGraph, d.source, d.secondPath.asScala.toSeq)))
  }

  private def getNoStrictDependencyViolation(transitiveClosure: ModuleGraphTransitiveClosure, rules: Seq[NoStrictDependency], other: HWModule): Seq[ModuleConnectionViolation] = {
    rules
      .filter(r => r.source != other && r.dest != other && transitiveClosure.minimumDistance(r.source, r.dest).get() == 1)
      .map(r => ModuleConnectionViolation(r.source.name, r.dest.name))
  }
}
