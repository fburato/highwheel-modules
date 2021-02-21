package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules.{
  EvidenceModuleDependency,
  HWModule,
  ModuleGraph,
  TrackingModuleDependency
}

class GuavaEvidenceModuleGraph(
  private val graph: ModuleGraph[TrackingModuleDependency],
  evidenceLimit: Option[Int]
) extends ModuleGraph[EvidenceModuleDependency] {
  override def findDependency(
    vertex1: HWModule,
    vertex2: HWModule
  ): Option[EvidenceModuleDependency] = {
    graph
      .findDependency(vertex1, vertex2)
      .map(e => EvidenceModuleDependency(e.source, e.dest, head(e.sources), head(e.destinations)))
  }

  private def head[T](s: Set[T]): T = s.toSeq.head

  override def addDependency(dependency: EvidenceModuleDependency): Unit = {
    if (Seq(dependency.destModule, dependency.sourceModule).forall(graph.modules.contains)) {
      val trackingModuleDependency =
        TrackingModuleDependency(dependency.sourceModule, dependency.destModule)
      val dependencyOptional = graph.findDependency(dependency.sourceModule, dependency.destModule)
      if (
        evidenceLimit
          .forall(limit => dependencyOptional.map(_.evidenceCounter).getOrElse(0) < limit)
      ) {
        trackingModuleDependency.addEvidence(dependency.source, dependency.dest)
      }
      graph.addDependency(trackingModuleDependency)
    }
  }

  override def addModule(vertex: HWModule): Unit = graph.addModule(vertex)

  override def modules: Seq[HWModule] = graph.modules

  override def dependencies(vertex: HWModule): Seq[HWModule] = graph.dependencies(vertex)
}
