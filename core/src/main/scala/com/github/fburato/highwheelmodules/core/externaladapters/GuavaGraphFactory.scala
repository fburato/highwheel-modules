package com.github.fburato.highwheelmodules.core.externaladapters

import java.util.Optional

import com.github.fburato.highwheelmodules.model.modules._
import com.google.common.graph.NetworkBuilder

import scala.jdk.OptionConverters._

class GuavaGraphFactory extends ModuleGraphFactory {
  override def buildMetricModuleGraph(): MetricModuleGraph[ModuleDependency] =
    new GuavaModuleGraph(NetworkBuilder.directed.allowsSelfLoops(true).build)

  override def buildEvidenceModuleGraph(trackingGraph: ModuleGraph[TrackingModuleDependency], evidenceLimit: Optional[Integer]): ModuleGraph[EvidenceModuleDependency] =
    new GuavaEvidenceModuleGraph(trackingGraph, evidenceLimit.toScala.map(_.toInt))

  override def buildTrackingModuleGraph(): ModuleGraph[TrackingModuleDependency] =
    new GuavaTrackingModuleGraph(NetworkBuilder.directed.allowsSelfLoops(true).build)
}
