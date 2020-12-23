package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor

class FilteringDecorator(child: AccessVisitor, filter: ElementName => Boolean) extends AccessVisitor {

  override def apply(source: AccessPoint, dest: AccessPoint, `type`: AccessType): Unit = {
    if (filter(dest.elementName) && filter(source.elementName)) {
      child.apply(source, dest, `type`)
    }
  }

  override def newNode(clazz: ElementName): Unit = {
    if (filter(clazz)) {
      child.newNode(clazz)
    }
  }

  override def newAccessPoint(ap: AccessPoint): Unit = {
    child.newAccessPoint(ap)
  }

  override def newEntryPoint(clazz: ElementName): Unit = {
    if (filter(clazz)) {
      child.newEntryPoint(clazz)
    }
  }
}
