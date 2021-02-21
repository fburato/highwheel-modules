package com.github.fburato.highwheelmodules.model.bytecode

case class AccessPointName private (name: String, descriptor: String)

object AccessPointName {
  def create(name: String, descriptor: String): AccessPointName =
    AccessPointName(name.replace('<', '(').replace('>', ')').intern(), descriptor)
}
