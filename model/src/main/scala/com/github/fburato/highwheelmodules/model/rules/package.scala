package com.github.fburato.highwheelmodules.model

import com.github.fburato.highwheelmodules.model.modules.HWModuleS

package object rules {

  sealed trait RuleS

  case class DependencyS(source: HWModuleS, dest: HWModuleS)

  case class NoStrictDependencyS(source: HWModuleS, dest: HWModuleS)

}
