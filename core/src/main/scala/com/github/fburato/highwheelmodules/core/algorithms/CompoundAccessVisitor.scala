package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor

class CompoundAccessVisitor(private val accessVisitors: Seq[AccessVisitor]) extends AccessVisitor {
  override def newNode(clazz: ElementName): Unit = accessVisitors.foreach(av => av.newNode(clazz))

  override def newAccessPoint(ap: AccessPoint): Unit = accessVisitors.foreach(av => av.newAccessPoint(ap))

  override def newEntryPoint(clazz: ElementName): Unit = accessVisitors.foreach(av => av.newEntryPoint(clazz))

  override def apply(source: AccessPoint, dest: AccessPoint, `type`: AccessType): Unit = accessVisitors.foreach(av => av.apply(source, dest, `type`))
}

object CompoundAccessVisitor {
  def apply(accessVisitors: Seq[AccessVisitor]): CompoundAccessVisitor = new CompoundAccessVisitor(accessVisitors)
}
