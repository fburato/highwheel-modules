package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.ElementName

trait NameTransformer {
  def transform(elementName: String): ElementName
}
