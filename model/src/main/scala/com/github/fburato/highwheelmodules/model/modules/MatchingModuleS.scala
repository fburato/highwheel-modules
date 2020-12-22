package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS

trait MatchingModuleS {
  def contains(elementName: ElementNameS): Boolean
}
