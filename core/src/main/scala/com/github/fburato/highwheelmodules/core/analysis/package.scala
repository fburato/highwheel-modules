package com.github.fburato.highwheelmodules.core


import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import com.github.fburato.highwheelmodules.model.modules._
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}

package object analysis {

  case class AnalysisState(modules: Seq[HWModuleS],
                           dependencies: Seq[DependencyS],
                           noStrictDependencies: Seq[NoStrictDependencyS],
                           specGraph: MetricModuleGraphS[ModuleDependencyS],
                           actualGraph: MetricModuleGraphS[ModuleDependencyS],
                           actualTrackingGraph: ModuleGraphS[TrackingModuleDependencyS],
                           visitor: AccessVisitor,
                           other: HWModuleS)

  case class Metric(module: String, fanIn: Int, fanOut: Int)

  case class EvidenceBackedViolation(sourceModule: String, destinationModule: String, specificationPath: Seq[String],
                                     actualPath: Seq[String], evidences: Seq[Seq[(String, String)]])

  case class ModuleConnectionViolation(sourceModule: String, destinationModule: String)

  case class AnalysisResult(evidenceBackedViolations: Seq[EvidenceBackedViolation],
                            moduleConnectionViolations: Seq[ModuleConnectionViolation], metrics: Seq[Metric])

  def getMetrics(moduleMetrics: HWModuleMetricS, modules: Seq[HWModuleS], graph: ModuleGraphS[ModuleDependencyS],
                 other: HWModuleS): Seq[Metric] = {
    modules.map(m => Metric(m.name,
      moduleMetrics.fanInOf(m).getOrElse(0) + graph.findDependency(other, m).map(_ => -1).getOrElse(0),
      moduleMetrics.fanOutOf(m).getOrElse(0) + graph.findDependency(m, other).map(_ => -1).getOrElse(0)
    ))
  }

  @inline def getNames(modules: Seq[HWModuleS]): Seq[String] = modules.map(_.name)

  def getEvidence(trackingGraph: ModuleGraphS[TrackingModuleDependencyS], source: HWModuleS,
                  path: Seq[HWModuleS]): Seq[Seq[(String, String)]] = {
    val completePath = Seq(source) ++ path
    completePath.zip(path).map {
      case (first, second) =>
        val maybeDependency = trackingGraph.findDependency(first, second)
        maybeDependency
          .map(dependency => dependency.sources.toSeq
            .flatMap(source => dependency.destinationsFromSource(source).toSeq
              .map(destination => (source.toString, destination.toString))))
          .getOrElse(Seq())
    }
  }
}
