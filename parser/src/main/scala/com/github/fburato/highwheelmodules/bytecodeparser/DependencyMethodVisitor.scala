package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode._
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm._

private[bytecodeparser] class DependencyMethodVisitor(parent: AccessPointS, typeReceiver: AccessVisitor, nameTransformer: NameTransformer)
  extends MethodVisitor(Opcodes.ASM8, null) {

  override def visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean): Unit = {
    typeReceiver(parent, AccessPointS(nameTransformer.transform(owner), AccessPointNameS.create(name, descriptor)), USES)
  }

  override def visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, bootstrapMethodArguments: Object*): Unit = {
    val method = Type.getMethodType(descriptor)
    method.getArgumentTypes.foreach(argumentType =>
      typeReceiver(parent, AccessPointS(nameTransformer.transform(argumentType.getClassName)), USES)
    )
    typeReceiver(parent, AccessPointS(nameTransformer.transform(method.getReturnType.getClassName)), USES)
    bootstrapMethodArguments.foreach {
      case h: Handle => typeReceiver(parent, AccessPointS(ElementNameS.fromString(h.getOwner), AccessPointNameS.create(h.getName, h.getDesc)), USES)
      case _ => ()
    }
  }

  override def visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String): Unit = {
    typeReceiver(parent, AccessPointS(nameTransformer.transform(owner), AccessPointNameS.create(name, descriptor)), USES)
  }

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    typeReceiver(parent, AccessPointS(ElementNameS.fromString(Type.getType(descriptor).getClassName)), ANNOTATED)
    null
  }

  override def visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor = {
    typeReceiver(parent, AccessPointS(ElementNameS.fromString(Type.getType(descriptor).getClassName)), ANNOTATED)
    null
  }

  override def visitLdcInsn(value: Object): Unit = value match {
    case t: Type =>
      val element = getElementNameForType(t)
      typeReceiver(parent, AccessPointS(element), USES)
    case _ => ()
  }

  override def visitLocalVariable(name: String, descriptor: String, signature: String, start: Label, end: Label, index: Int): Unit = {
    val element = ElementNameS.fromString(Type.getType(descriptor).getClassName)
    typeReceiver.apply(parent, AccessPointS(element), USES)
  }
}
