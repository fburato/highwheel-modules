package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementName

trait MatchingModule {
  def contains(elementName: ElementName): Boolean
}
