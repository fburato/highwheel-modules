package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, AccessTypeS, ElementNameS}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor

import java.util.function.Predicate

class FilteringDecorator(child: AccessVisitor, filter: ElementNameS => Boolean) extends AccessVisitor {

  def this(child: AccessVisitor, filter: Predicate[ElementNameS]) = this(child, el => filter.test(el)) // TODO remove after rewrite

  override def apply(source: AccessPointS, dest: AccessPointS, `type`: AccessTypeS): Unit = {
    if (filter(dest.elementName) && filter(source.elementName)) {
      child.apply(source, dest, `type`)
    }
  }

  override def newNode(clazz: ElementNameS): Unit = {
    if (filter(clazz)) {
      child.newNode(clazz)
    }
  }

  override def newAccessPoint(ap: AccessPointS): Unit = {
    child.newAccessPoint(ap)
  }

  override def newEntryPoint(clazz: ElementNameS): Unit = {
    if (filter(clazz)) {
      child.newEntryPoint(clazz)
    }
  }
}
