package com.github.fburato.highwheelmodules.model.classpath

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}

trait AccessVisitor {
  def apply(source: AccessPoint, dest: AccessPoint, accessType: AccessType): Unit

  def newNode(clazz: ElementName): Unit

  def newAccessPoint(ap: AccessPoint): Unit

  def newEntryPoint(clazz: ElementName): Unit
}
