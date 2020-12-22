package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.analysis.{AnalysisModeS, STRICT}
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}

case class DefinitionS(
                        whitelist: Option[AnonymousModuleS] = None,
                        blacklist: Option[AnonymousModuleS] = None,
                        mode: AnalysisModeS = STRICT,
                        modules: Seq[HWModuleS] = Seq(),
                        dependencies: Seq[DependencyS] = Seq(),
                        noStrictDependencies: Seq[NoStrictDependencyS] = Seq()
                      )
