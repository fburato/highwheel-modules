package com.github.fburato.highwheelmodules.model.modules

trait ModuleGraphFactory {

  def buildMetricModuleGraph: MetricModuleGraph[ModuleDependency]

  def buildEvidenceModuleGraph(
    moduleGraph: ModuleGraph[TrackingModuleDependency],
    evidenceLimit: Option[Int]
  ): ModuleGraph[EvidenceModuleDependency]

  def buildTrackingModuleGraph: ModuleGraph[TrackingModuleDependency]
}
