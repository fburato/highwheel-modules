package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules.{HWModuleS, ModuleGraphS, TrackingModuleDependencyS}
import com.google.common.graph.MutableNetwork

import java.util
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

class GuavaTrackingModuleGraph(private val graph: MutableNetwork[HWModuleS, TrackingModuleDependencyS]) extends ModuleGraphS[TrackingModuleDependencyS] {

  override def findDependency(vertex1: HWModuleS, vertex2: HWModuleS): Option[TrackingModuleDependencyS] = {
    if (graph.nodes().containsAll(list(vertex1, vertex2))) {
      graph.edgeConnecting(vertex1, vertex2).toScala
    } else {
      None
    }
  }

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: TrackingModuleDependencyS): Unit = {
    if (graph.nodes().containsAll(list(dependency.source, dependency.dest))) {
      val dep = graph.edgeConnecting(dependency.source, dependency.dest)
        .orElseGet(() => {
          val newDep = TrackingModuleDependencyS(dependency.source, dependency.dest)
          graph.addEdge(dependency.source, dependency.dest, newDep)
          newDep
        })
      for {
        s <- dependency.sources
        d <- dependency.destinationsFromSource(s)
      } dep.addEvidence(s, d)
    }
  }

  override def addModule(vertex: HWModuleS): Unit = graph.addNode(vertex)

  override def modules: Seq[HWModuleS] = graph.nodes().asScala.toSeq

  override def dependencies(vertex: HWModuleS): Seq[HWModuleS] = {
    if (graph.nodes().contains(vertex)) {
      graph.successors(vertex).asScala.toSeq
    } else {
      Seq()
    }
  }
}
