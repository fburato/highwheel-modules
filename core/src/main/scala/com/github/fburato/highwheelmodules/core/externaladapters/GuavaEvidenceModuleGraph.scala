package com.github.fburato.highwheelmodules.core.externaladapters

import java.util
import java.util.Optional

import com.github.fburato.highwheelmodules.model.modules.{EvidenceModuleDependency, HWModule, ModuleGraph, TrackingModuleDependency}

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class GuavaEvidenceModuleGraph(private val graph: ModuleGraph[TrackingModuleDependency], evidenceLimit: Option[Int]) extends ModuleGraph[EvidenceModuleDependency] {
  override def findDependency(vertex1: HWModule, vertex2: HWModule): Optional[EvidenceModuleDependency] = {
    graph.findDependency(vertex1, vertex2)
      .map(e => new EvidenceModuleDependency(e.source, e.dest, head(e.getSources), head(e.getDestinations)))
  }

  private def head[T](s: util.Set[T]): T = s.asScala.toSeq.head

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: EvidenceModuleDependency): Unit = {
    if (graph.modules().containsAll(list(dependency.destModule, dependency.sourceModule))) {
      val trackingModuleDependency = new TrackingModuleDependency(dependency.sourceModule, dependency.destModule)
      val dependencyOptional = graph.findDependency(dependency.sourceModule, dependency.destModule).toScala
      if (evidenceLimit.forall(limit => dependencyOptional.map(_.getEvidenceCounter).getOrElse(0) < limit)) {
        trackingModuleDependency.addEvidence(dependency.source, dependency.dest)
      }
      graph.addDependency(trackingModuleDependency)
    }
  }

  override def addModule(vertex: HWModule): Unit = graph.addModule(vertex)

  override def modules(): util.Collection[HWModule] = graph.modules()

  override def dependencies(vertex: HWModule): util.Collection[HWModule] = graph.dependencies(vertex)
}
