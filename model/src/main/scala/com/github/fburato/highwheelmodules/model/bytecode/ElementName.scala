package com.github.fburato.highwheelmodules.model.bytecode

case class ElementName private (private val name: String) {
  def asJavaName: String = name.replace('/', '.')

  def asInternalName: String = name
}

object ElementName {
  def fromString(clazz: String): ElementName = ElementName(removeSymbols(clazz))

  private def removeSymbols(name: String): String = name.replace('.', '/').intern()

  def fromClass(clazz: Class[_]): ElementName = ElementName(removeSymbols(clazz.getName))

  implicit val ordering: Ordering[ElementName] = (x: ElementName, y: ElementName) =>
    x.name compareTo y.name
}
