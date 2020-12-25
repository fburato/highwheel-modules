package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementName

case class HWModule private(name: String, private val anonymousModule: AnonymousModule) extends MatchingModule {
  override def contains(elementName: ElementName): Boolean = anonymousModule.contains(elementName)
}

object HWModule {
  def make(moduleName: String, globs: Seq[String]): Option[HWModule] = AnonymousModule.make(globs)
    .map(anonymousModule => HWModule(moduleName, anonymousModule))
}
