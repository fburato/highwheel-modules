package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS

trait NameTransformer {
  def transform(elementName: String): ElementNameS
}
