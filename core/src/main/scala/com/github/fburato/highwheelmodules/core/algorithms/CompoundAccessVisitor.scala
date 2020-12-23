package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, AccessTypeS, ElementNameS}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor

class CompoundAccessVisitor(private val accessVisitors: Seq[AccessVisitor]) extends AccessVisitor {
  override def newNode(clazz: ElementNameS): Unit = accessVisitors.foreach(av => av.newNode(clazz))

  override def newAccessPoint(ap: AccessPointS): Unit = accessVisitors.foreach(av => av.newAccessPoint(ap))

  override def newEntryPoint(clazz: ElementNameS): Unit = accessVisitors.foreach(av => av.newEntryPoint(clazz))

  override def apply(source: AccessPointS, dest: AccessPointS, accessType: AccessTypeS): Unit = accessVisitors.foreach(av => av.apply(source, dest, accessType))
}

object CompoundAccessVisitor {
  def apply(accessVisitors: Seq[AccessVisitor]): CompoundAccessVisitor = new CompoundAccessVisitor(accessVisitors)
}
