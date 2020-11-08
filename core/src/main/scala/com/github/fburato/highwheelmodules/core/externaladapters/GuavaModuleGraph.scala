package com.github.fburato.highwheelmodules.core.externaladapters

import java.util
import java.util.{Collections, Optional}

import com.github.fburato.highwheelmodules.model.modules.{HWModule, MetricModuleGraph, ModuleDependency}
import com.google.common.graph.MutableNetwork

import scala.jdk.CollectionConverters._

class GuavaModuleGraph(private val graph: MutableNetwork[HWModule, ModuleDependency]) extends MetricModuleGraph[ModuleDependency] {
  override def fanInOf(module: HWModule): Optional[Integer] = {
    if (!graph.nodes().contains(module)) {
      Optional.empty()
    } else {
      Optional.of(findDependency(module, module)
        .map(_ => graph.inDegree(module) - 1)
        .orElseGet(() => graph.inDegree(module)))
    }
  }

  override def fanOutOf(module: HWModule): Optional[Integer] = {
    if (!graph.nodes().contains(module)) {
      Optional.empty()
    } else {
      Optional.of(findDependency(module, module)
        .map(_ => graph.outDegree(module) - 1)
        .orElseGet(() => graph.outDegree(module)))
    }
  }

  override def findDependency(vertex1: HWModule, vertex2: HWModule): Optional[ModuleDependency] = {
    if (graph.nodes().containsAll(list(vertex1, vertex2))) {
      graph.edgeConnecting(vertex1, vertex2)
    } else {
      Optional.empty()
    }
  }

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: ModuleDependency): Unit = {
    if (graph.nodes().containsAll(list(dependency.source, dependency.dest))) {
      val moduleDependency = graph.edgeConnecting(dependency.source, dependency.dest)
        .orElseGet(() => {
          graph.addEdge(dependency.source, dependency.dest, dependency)
          dependency
        })
      moduleDependency.incrementCount()
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
