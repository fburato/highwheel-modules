package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.bytecodeparser.DependencyClassVisitor.filterOutJavaLangObject
import com.github.fburato.highwheelmodules.model.bytecode._
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm._
import org.objectweb.asm.signature.SignatureReader

private[bytecodeparser] class DependencyClassVisitor(classVisitor: ClassVisitor, typeReceiver: AccessVisitor, nameTransformer: NameTransformer)
  extends ClassVisitor(Opcodes.ASM8, classVisitor) {
  private val dependencyVisitor = filterOutJavaLangObject(typeReceiver)
  private var parent: AccessPointS = null

  override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String]): Unit = {
    parent = AccessPointS(nameTransformer.transform(name))
    dependencyVisitor.newNode(parent.elementName)

    if (superName != null) {
      dependencyVisitor(parent, AccessPointS(nameTransformer.transform(superName)), INHERITANCE)
    }
    interfaces.foreach(interface =>
      dependencyVisitor(parent, AccessPointS(nameTransformer.transform(interface)), IMPLEMENTS)
    )
    visitSignatureWithAccessType(signature, SIGNATURE)
  }

  private def visitSignatureWithAccessType(signature: String, accessType: AccessTypeS): Unit = {
    if (signature != null) {
      val signatureReader = new SignatureReader(signature)
      signatureReader.accept(new DependencySignatureVisitor(parent, dependencyVisitor, accessType))
    }
  }

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    dependencyVisitor(parent, AccessPointS(ElementNameS.fromString(Type.getType(descriptor).getClassName)), ANNOTATED)
    null
  }

  override def visitField(access: Int, name: String, descriptor: String, signature: String, value: Object): FieldVisitor = {
    val asmType = Type.getType(descriptor)
    dependencyVisitor(parent, AccessPointS(getElementNameForType(asmType)), COMPOSED)
    visitSignatureWithAccessType(signature, COMPOSED)
    new DependencyFieldVisitor(parent, dependencyVisitor)
  }

  override def visitOuterClass(owner: String, name: String, descriptor: String): Unit = {
    val outer = nameTransformer.transform(owner)
    if (name != null) {
      parent = AccessPointS(outer, AccessPointNameS.create(name, descriptor))
    } else {
      parent = AccessPointS(outer)
    }
  }

  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    def pickAccessPointForMethod: AccessPointS = {
      if (parent.attribute != null) {
        parent
      } else {
        parent.copy(attribute = AccessPointNameS.create(name, descriptor))
      }
    }

    def examineParameters(method: AccessPointS): Unit = {
      val parameters = Type.getArgumentTypes(descriptor)
      parameters.foreach(param =>
        dependencyVisitor(method, AccessPointS(nameTransformer.transform(getElementNameForType(param).asInternalName)), SIGNATURE)
      )
    }

    def examineExceptions(method: AccessPointS): Unit = {
      if (exceptions != null) {
        exceptions.foreach(exception =>
          dependencyVisitor(method, AccessPointS(nameTransformer.transform(exception)), SIGNATURE)
        )
      }
    }

    def examineReturnType(method: AccessPointS): Unit = {
      val returnType = Type.getMethodType(descriptor).getReturnType
      dependencyVisitor(method, AccessPointS(nameTransformer.transform(getElementNameForType(returnType).asInternalName)), SIGNATURE)
    }

    def isEntryPoint: Boolean = {
      (Opcodes.ACC_STATIC & access) != 0 && name == "main" && descriptor == "([Ljava/lang/String;)V"
    }

    val method = pickAccessPointForMethod

    dependencyVisitor.newAccessPoint(method)
    examineParameters(method)
    examineExceptions(method)
    examineReturnType(method)

    if (isEntryPoint) {
      dependencyVisitor.newEntryPoint(parent.elementName)
    }

    visitSignatureWithAccessType(signature, SIGNATURE)

    new DependencyMethodVisitor(method, dependencyVisitor, nameTransformer)
  }
}

object DependencyClassVisitor {
  private val OBJECT = ElementNameS.fromClass(classOf[Object])

  private def filterOutJavaLangObject(delegate: AccessVisitor): AccessVisitor = new AccessVisitor {
    override def apply(source: AccessPointS, dest: AccessPointS, accessType: AccessTypeS): Unit =
      if (!(dest.elementName == OBJECT)) {
        delegate.apply(source, dest, accessType)
      }

    override def newNode(clazz: ElementNameS): Unit = delegate.newNode(clazz)

    override def newAccessPoint(ap: AccessPointS): Unit = delegate.newAccessPoint(ap)

    override def newEntryPoint(clazz: ElementNameS): Unit = delegate.newEntryPoint(clazz)
  }
}
