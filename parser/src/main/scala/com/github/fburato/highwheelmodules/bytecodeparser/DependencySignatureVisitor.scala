package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

private[bytecodeparser] class DependencySignatureVisitor(
  parent: AccessPoint,
  typeReceiver: AccessVisitor,
  accessType: AccessType
) extends SignatureVisitor(Opcodes.ASM8) {

  override def visitClassType(name: String): Unit = {
    typeReceiver(parent, AccessPoint(ElementName.fromString(name)), accessType)
  }
}
