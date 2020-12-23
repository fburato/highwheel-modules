package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules._
import com.google.common.graph.NetworkBuilder

class GuavaGraphFactory extends ModuleGraphFactory {
  override def buildMetricModuleGraph: MetricModuleGraph[ModuleDependency] =
    new GuavaModuleGraph(NetworkBuilder.directed.allowsSelfLoops(true).build)

  override def buildEvidenceModuleGraph(trackingGraph: ModuleGraph[TrackingModuleDependency], evidenceLimit: Option[Int]): ModuleGraph[EvidenceModuleDependency] =
    new GuavaEvidenceModuleGraph(trackingGraph, evidenceLimit)

  override def buildTrackingModuleGraph: ModuleGraph[TrackingModuleDependency] =
    new GuavaTrackingModuleGraph(NetworkBuilder.directed.allowsSelfLoops(true).build)
}
