package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules.{EvidenceModuleDependencyS, HWModuleS, ModuleGraphS, TrackingModuleDependencyS}

class GuavaEvidenceModuleGraph(private val graph: ModuleGraphS[TrackingModuleDependencyS], evidenceLimit: Option[Int]) extends ModuleGraphS[EvidenceModuleDependencyS] {
  override def findDependency(vertex1: HWModuleS, vertex2: HWModuleS): Option[EvidenceModuleDependencyS] = {
    graph.findDependency(vertex1, vertex2)
      .map(e => EvidenceModuleDependencyS(e.source, e.dest, head(e.sources), head(e.destinations)))
  }

  private def head[T](s: Set[T]): T = s.toSeq.head


  override def addDependency(dependency: EvidenceModuleDependencyS): Unit = {
    if (Seq(dependency.destModule, dependency.sourceModule).forall(graph.modules.contains)) {
      val trackingModuleDependency = TrackingModuleDependencyS(dependency.sourceModule, dependency.destModule)
      val dependencyOptional = graph.findDependency(dependency.sourceModule, dependency.destModule)
      if (evidenceLimit.forall(limit => dependencyOptional.map(_.evidenceCounter).getOrElse(0) < limit)) {
        trackingModuleDependency.addEvidence(dependency.source, dependency.dest)
      }
      graph.addDependency(trackingModuleDependency)
    }
  }

  override def addModule(vertex: HWModuleS): Unit = graph.addModule(vertex)

  override def modules: Seq[HWModuleS] = graph.modules

  override def dependencies(vertex: HWModuleS): Seq[HWModuleS] = graph.dependencies(vertex)
}
