package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{ANNOTATED, AccessPointS, ElementNameS}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm.{AnnotationVisitor, FieldVisitor, Opcodes, Type}

private[bytecodeparser] class DependencyFieldVisitor(parent: AccessPointS, accessVisitor: AccessVisitor) extends FieldVisitor(Opcodes.ASM8, null) {

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    accessVisitor(parent, AccessPointS(ElementNameS.fromString(Type.getType(descriptor).getClassName)), ANNOTATED)
    null
  }
}
