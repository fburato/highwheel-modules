package com.github.fburato.highwheelmodules.model

import com.github.fburato.highwheelmodules.model.modules.HWModule

package object rules {

  sealed trait RuleS

  case class DependencyS(source: HWModule, dest: HWModule)

  case class NoStrictDependencyS(source: HWModule, dest: HWModule)

}
