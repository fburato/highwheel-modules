package com.github.fburato.highwheelmodules.model.modules

trait ModuleGraphFactoryS {

  def buildMetricModuleGraph: MetricModuleGraphS[ModuleDependencyS]

  def buildEvidenceModuleGraph(moduleGraph: ModuleGraphS[TrackingModuleDependencyS], evidenceLimit: Option[Int]): ModuleGraphS[EvidenceModuleDependencyS]

  def buildTrackingModuleGraph: ModuleGraphS[TrackingModuleDependencyS]
}
