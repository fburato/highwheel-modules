package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.analysis.{AnalysisMode, STRICT}
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}

case class Definition(
  whitelist: Option[AnonymousModule] = None,
  blacklist: Option[AnonymousModule] = None,
  mode: AnalysisMode = STRICT,
  modules: Seq[HWModule] = Seq(),
  dependencies: Seq[DependencyS] = Seq(),
  noStrictDependencies: Seq[NoStrictDependencyS] = Seq()
)
