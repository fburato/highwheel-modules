package com.github.fburato.highwheelmodules.model.bytecode

case class AccessPointNameS private(name: String, descriptor: String)

object AccessPointNameS {
  def create(name: String, descriptor: String): AccessPointNameS = AccessPointNameS(
    name.replace('<', '(').replace('>', ')').intern(), descriptor
  )
}
