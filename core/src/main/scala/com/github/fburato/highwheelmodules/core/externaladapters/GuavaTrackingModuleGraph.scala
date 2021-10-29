package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules.{
  HWModule,
  ModuleGraph,
  TrackingModuleDependency
}
import com.google.common.graph.MutableNetwork

import java.util
import scala.collection.JavaConverters._
import com.github.fburato.highwheelmodules.utils.OptionConverters._

class GuavaTrackingModuleGraph(
  private val graph: MutableNetwork[HWModule, TrackingModuleDependency]
) extends ModuleGraph[TrackingModuleDependency] {

  override def findDependency(
    vertex1: HWModule,
    vertex2: HWModule
  ): Option[TrackingModuleDependency] = {
    if (graph.nodes().containsAll(list(vertex1, vertex2))) {
      graph.edgeConnecting(vertex1, vertex2).toScala
    } else {
      None
    }
  }

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: TrackingModuleDependency): Unit = {
    if (graph.nodes().containsAll(list(dependency.source, dependency.dest))) {
      val dep = graph
        .edgeConnecting(dependency.source, dependency.dest)
        .orElseGet(() => {
          val newDep = TrackingModuleDependency(dependency.source, dependency.dest)
          graph.addEdge(dependency.source, dependency.dest, newDep)
          newDep
        })
      for {
        s <- dependency.sources
        d <- dependency.destinationsFromSource(s)
      } dep.addEvidence(s, d)
    }
  }

  override def addModule(vertex: HWModule): Unit = graph.addNode(vertex)

  override def modules: Seq[HWModule] = graph.nodes().asScala.toSeq

  override def dependencies(vertex: HWModule): Seq[HWModule] = {
    if (graph.nodes().contains(vertex)) {
      graph.successors(vertex).asScala.toSeq
    } else {
      Seq()
    }
  }
}
