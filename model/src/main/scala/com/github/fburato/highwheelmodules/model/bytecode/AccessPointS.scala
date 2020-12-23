package com.github.fburato.highwheelmodules.model.bytecode

case class AccessPointS(elementName: ElementNameS, attribute: AccessPointNameS = null) {
  override def toString: String =
    if (attribute != null) {
      s"${elementName.asJavaName}:${attribute.name}"
    } else {
      elementName.asJavaName
    }
}
