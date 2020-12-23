package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, AccessTypeS, ElementNameS}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

private[bytecodeparser] class DependencySignatureVisitor(parent: AccessPointS, typeReceiver: AccessVisitor, accessType: AccessTypeS) extends SignatureVisitor(Opcodes.ASM8) {

  override def visitClassType(name: String): Unit = {
    typeReceiver(parent, AccessPointS(ElementNameS.fromString(name)), accessType)
  }
}
