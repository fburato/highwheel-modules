package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessPointName, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm._

private[bytecodeparser] class DependencyMethodVisitor(parent: AccessPoint, typeReceiver: AccessVisitor, nameTransformer: NameTransformer)
  extends MethodVisitor(Opcodes.ASM8, null) {

  override def visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean): Unit = {
    typeReceiver(parent, AccessPoint.create(nameTransformer.transform(owner), AccessPointName.create(name, descriptor)), AccessType.USES)
  }

  override def visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, bootstrapMethodArguments: Object*): Unit = {
    val method = Type.getMethodType(descriptor)
    method.getArgumentTypes.foreach(argumentType =>
      typeReceiver(parent, AccessPoint.create(nameTransformer.transform(argumentType.getClassName)), AccessType.USES)
    )
    typeReceiver(parent, AccessPoint.create(nameTransformer.transform(method.getReturnType.getClassName)), AccessType.USES)
    bootstrapMethodArguments.foreach {
      case h: Handle => typeReceiver(parent, AccessPoint.create(ElementName.fromString(h.getOwner), AccessPointName.create(h.getName, h.getDesc)), AccessType.USES)
      case _ => ()
    }
  }

  override def visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String): Unit = {
    typeReceiver(parent, AccessPoint.create(nameTransformer.transform(owner), AccessPointName.create(name, descriptor)), AccessType.USES)
  }

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    typeReceiver(parent, AccessPoint.create(ElementName.fromString(Type.getType(descriptor).getClassName)), AccessType.ANNOTATED)
    null
  }

  override def visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor = {
    typeReceiver(parent, AccessPoint.create(ElementName.fromString(Type.getType(descriptor).getClassName)), AccessType.ANNOTATED)
    null
  }

  override def visitLdcInsn(value: Object): Unit = value match {
    case t: Type =>
      val element = getElementNameForType(t)
      typeReceiver(parent, AccessPoint.create(element), AccessType.USES)
    case _ => ()
  }

  override def visitLocalVariable(name: String, descriptor: String, signature: String, start: Label, end: Label, index: Int): Unit = {
    val element = ElementName.fromString(Type.getType(descriptor).getClassName)
    typeReceiver.apply(parent, AccessPoint.create(element), AccessType.USES)
  }
}
