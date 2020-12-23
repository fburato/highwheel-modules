package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules._
import com.google.common.graph.MutableNetwork

import java.util
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

class GuavaModuleGraph(private val graph: MutableNetwork[HWModuleS, ModuleDependencyS]) extends MetricModuleGraphS[ModuleDependencyS] {
  override def fanInOf(module: HWModuleS): Option[Int] = {
    if (!graph.nodes().contains(module)) {
      None
    } else {
      Some(findDependency(module, module)
        .map(_ => graph.inDegree(module) - 1)
        .getOrElse(graph.inDegree(module))
      )
    }
  }

  override def fanOutOf(module: HWModuleS): Option[Int] = {
    if (!graph.nodes().contains(module)) {
      None
    } else {
      Some(findDependency(module, module)
        .map(_ => graph.outDegree(module) - 1)
        .getOrElse(graph.outDegree(module))
      )
    }
  }

  override def findDependency(vertex1: HWModuleS, vertex2: HWModuleS): Option[ModuleDependencyS] = {
    if (graph.nodes().containsAll(list(vertex1, vertex2))) {
      graph.edgeConnecting(vertex1, vertex2).toScala
    } else {
      None
    }
  }

  private def list[T](ts: T*): util.List[T] = ts.asJava

  override def addDependency(dependency: ModuleDependencyS): Unit = {
    if (Seq(dependency.dest, dependency.source).forall(graph.nodes().contains)) {
      val moduleDependency = graph.edgeConnecting(dependency.source, dependency.dest)
        .orElseGet(() => {
          graph.addEdge(dependency.source, dependency.dest, dependency)
          dependency
        })
      moduleDependency.incrementCount()
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
