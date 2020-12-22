package com.github.fburato.highwheelmodules.model.bytecode

case class ElementNameS private(private val name: String) {
  def asJavaName: String = name.replace('/', '.')

  def asInternalName: String = name
}

object ElementNameS {
  def fromString(clazz: String): ElementNameS = ElementNameS(removeSymbols(clazz))

  private def removeSymbols(name: String): String = name.replace('.', '/').intern()

  def fromClass(clazz: Class[_]): ElementNameS = ElementNameS(removeSymbols(clazz.getName))

  implicit val ordering: Ordering[ElementNameS] = (x: ElementNameS, y: ElementNameS) => x.name compareTo y.name
}
