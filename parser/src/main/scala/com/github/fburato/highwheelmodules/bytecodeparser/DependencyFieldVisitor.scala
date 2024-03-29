package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{ANNOTATED, AccessPoint, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm.{AnnotationVisitor, FieldVisitor, Opcodes, Type}

private[bytecodeparser] class DependencyFieldVisitor(
  parent: AccessPoint,
  accessVisitor: AccessVisitor
) extends FieldVisitor(Opcodes.ASM9, null) {

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    accessVisitor(
      parent,
      AccessPoint(ElementName.fromString(Type.getType(descriptor).getClassName)),
      ANNOTATED
    )
    null
  }
}
