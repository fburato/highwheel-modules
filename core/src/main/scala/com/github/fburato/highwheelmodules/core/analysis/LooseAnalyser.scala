package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure
import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleGraph, TrackingModuleDependency}
import com.github.fburato.highwheelmodules.model.rules.{Dependency, NoStrictDependency}

import scala.jdk.CollectionConverters._

private[analysis] object LooseAnalyser extends Analyser {
  override def analyse(state: AnalysisState): AnalysisResult = {
    val actualTransitiveClosure = new ModuleGraphTransitiveClosure(state.actualGraph, (state.modules ++ Seq(state.other)).asJavaCollection)
    val absentDependencies = getAbsentDependencies(actualTransitiveClosure, state.dependencies, state.other)
    val undesiredDependencies = getUndesiredDependencies(actualTransitiveClosure, state.noStrictDependencies, state.other, state.actualTrackingGraph)

    AnalysisResult(undesiredDependencies,
      absentDependencies, getMetrics(state.actualGraph, state.modules, state.actualGraph, state.other))
  }

  private def getAbsentDependencies(transitiveClosure: ModuleGraphTransitiveClosure,
                                    dependencies: Seq[Dependency],
                                    other: HWModule): Seq[ModuleConnectionViolation] = {
    dependencies
      .filter(d => d.source != other && d.dest != other && !transitiveClosure.isReachable(d.source, d.dest))
      .map(d => ModuleConnectionViolation(d.source.name, d.dest.name))
  }

  private def getUndesiredDependencies(transitiveClosure: ModuleGraphTransitiveClosure,
                                       noStrictDependencies: Seq[NoStrictDependency],
                                       other: HWModule,
                                       trackingGraph: ModuleGraph[TrackingModuleDependency]): Seq[EvidenceBackedViolation] = {
    noStrictDependencies
      .filter(d => d.source != other && d.dest != other && transitiveClosure.isReachable(d.source, d.dest))
      .map(d => EvidenceBackedViolation(d.source.name, d.dest.name,
        List(d.source.name, d.dest.name),
        getNames(transitiveClosure.minimumDistancePath(d.source, d.dest).asScala.toSeq),
        getEvidence(trackingGraph, d.source, transitiveClosure.minimumDistancePath(d.source, d.dest).asScala.toSeq)))
  }
}
