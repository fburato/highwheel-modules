package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor

import java.util.function.Predicate

class FilteringDecorator(child: AccessVisitor, filter: ElementName => Boolean) extends AccessVisitor {

  def this(child: AccessVisitor, filter: Predicate[ElementName]) = this(child, el => filter.test(el)) // TODO remove after rewrite

  override def apply(source: AccessPoint, dest: AccessPoint, `type`: AccessType): Unit = {
    if (filter(dest.getElementName) && filter(source.getElementName)) {
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
