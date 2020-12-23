package com.github.fburato.highwheelmodules.model.classpath

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, AccessTypeS, ElementNameS}

trait AccessVisitor {
  def apply(source: AccessPointS, dest: AccessPointS, accessType: AccessTypeS): Unit

  def newNode(clazz: ElementNameS): Unit

  def newAccessPoint(ap: AccessPointS): Unit

  def newEntryPoint(clazz: ElementNameS): Unit
}
