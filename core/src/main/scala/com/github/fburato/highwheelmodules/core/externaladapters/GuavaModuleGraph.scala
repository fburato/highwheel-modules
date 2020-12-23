package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules._
import com.google.common.graph.MutableNetwork

import java.util
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

class GuavaModuleGraph(private val graph: MutableNetwork[HWModule, ModuleDependency]) extends MetricModuleGraph[ModuleDependency] {
  override def fanInOf(module: HWModule): Option[Int] = {
    if (!graph.nodes().contains(module)) {
      None
    } else {
      Some(findDependency(module, module)
        .map(_ => graph.inDegree(module) - 1)
        .getOrElse(graph.inDegree(module))
      )
    }
  }

  override def fanOutOf(module: HWModule): Option[Int] = {
    if (!graph.nodes().contains(module)) {
      None
    } else {
      Some(findDependency(module, module)
        .map(_ => graph.outDegree(module) - 1)
        .getOrElse(graph.outDegree(module))
      )
    }
  }

  override def findDependency(vertex1: HWModule, vertex2: HWModule): Option[ModuleDependency] = {
    if (graph.nodes().containsAll(list(vertex1, vertex2))) {
      graph.edgeConnecting(vertex1, vertex2).toScala
    } else {
      None
    }
  }

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: ModuleDependency): Unit = {
    if (Seq(dependency.dest, dependency.source).forall(graph.nodes().contains)) {
      val moduleDependency = graph.edgeConnecting(dependency.source, dependency.dest)
        .orElseGet(() => {
          graph.addEdge(dependency.source, dependency.dest, dependency)
          dependency
        })
      moduleDependency.incrementCount()
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
