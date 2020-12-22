package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS

case class HWModuleS private(name: String, private val anonymousModule: AnonymousModuleS) extends MatchingModuleS {
  override def contains(elementName: ElementNameS): Boolean = anonymousModule.contains(elementName)
}

object HWModuleS {
  def make(moduleName: String, globs: Seq[String]): Option[HWModuleS] = AnonymousModuleS.make(globs)
    .map(anonymousModule => HWModuleS(moduleName, anonymousModule))
}
