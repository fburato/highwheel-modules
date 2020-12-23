package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules._
import com.google.common.graph.NetworkBuilder

class GuavaGraphFactory extends ModuleGraphFactoryS {
  override def buildMetricModuleGraph: MetricModuleGraphS[ModuleDependencyS] =
    new GuavaModuleGraph(NetworkBuilder.directed.allowsSelfLoops(true).build)

  override def buildEvidenceModuleGraph(trackingGraph: ModuleGraphS[TrackingModuleDependencyS], evidenceLimit: Option[Int]): ModuleGraphS[EvidenceModuleDependencyS] =
    new GuavaEvidenceModuleGraph(trackingGraph, evidenceLimit)

  override def buildTrackingModuleGraph: ModuleGraphS[TrackingModuleDependencyS] =
    new GuavaTrackingModuleGraph(NetworkBuilder.directed.allowsSelfLoops(true).build)
}
