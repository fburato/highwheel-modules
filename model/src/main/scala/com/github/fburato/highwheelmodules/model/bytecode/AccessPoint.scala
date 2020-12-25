package com.github.fburato.highwheelmodules.model.bytecode

case class AccessPoint(elementName: ElementName, attribute: AccessPointName = null) {
  override def toString: String =
    if (attribute != null) {
      s"${elementName.asJavaName}:${attribute.name}"
    } else {
      elementName.asJavaName
    }
}
