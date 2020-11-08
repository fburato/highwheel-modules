package com.github.fburato.highwheelmodules.core.externaladapters

import java.util
import java.util.{Collections, Optional}

import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleGraph, TrackingModuleDependency}
import com.google.common.graph.MutableNetwork

import scala.jdk.CollectionConverters._

class GuavaTrackingModuleGraph(private val graph: MutableNetwork[HWModule, TrackingModuleDependency]) extends ModuleGraph[TrackingModuleDependency] {

  override def findDependency(vertex1: HWModule, vertex2: HWModule): Optional[TrackingModuleDependency] = {
    if (graph.nodes().containsAll(list(vertex1, vertex2))) {
      graph.edgeConnecting(vertex1, vertex2)
    } else {
      Optional.empty()
    }
  }

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: TrackingModuleDependency): Unit = {
    if (graph.nodes().containsAll(list(dependency.source, dependency.dest))) {
      val dep = graph.edgeConnecting(dependency.source, dependency.dest)
        .orElseGet(() => {
          val newDep = new TrackingModuleDependency(dependency.source, dependency.dest)
          graph.addEdge(dependency.source, dependency.dest, newDep)
          newDep
        })
      for {
        s <- dependency.getSources.asScala
        d <- dependency.getDestinationsFromSource(s).asScala
      } dep.addEvidence(s, d)
    }
  }

  override def addModule(vertex: HWModule): Unit = graph.addNode(vertex)

  override def modules(): util.Collection[HWModule] = graph.nodes()

  override def dependencies(vertex: HWModule): util.Collection[HWModule] = {
    if (graph.nodes().contains(vertex)) {
      graph.successors(vertex)
    } else {
      Collections.emptySet()
    }
  }
}
