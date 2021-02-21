package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.bytecodeparser.DependencyClassVisitor.filterOutJavaLangObject
import com.github.fburato.highwheelmodules.model.bytecode._
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.objectweb.asm._
import org.objectweb.asm.signature.SignatureReader

private[bytecodeparser] class DependencyClassVisitor(
  classVisitor: ClassVisitor,
  typeReceiver: AccessVisitor,
  nameTransformer: NameTransformer
) extends ClassVisitor(Opcodes.ASM8, classVisitor) {
  private val dependencyVisitor = filterOutJavaLangObject(typeReceiver)
  private var parent: AccessPoint = null

  override def visit(
    version: Int,
    access: Int,
    name: String,
    signature: String,
    superName: String,
    interfaces: Array[String]
  ): Unit = {
    parent = AccessPoint(nameTransformer.transform(name))
    dependencyVisitor.newNode(parent.elementName)

    if (superName != null) {
      dependencyVisitor(parent, AccessPoint(nameTransformer.transform(superName)), INHERITANCE)
    }
    interfaces.foreach(interface =>
      dependencyVisitor(parent, AccessPoint(nameTransformer.transform(interface)), IMPLEMENTS)
    )
    visitSignatureWithAccessType(signature, SIGNATURE)
  }

  private def visitSignatureWithAccessType(signature: String, accessType: AccessType): Unit = {
    if (signature != null) {
      val signatureReader = new SignatureReader(signature)
      signatureReader.accept(new DependencySignatureVisitor(parent, dependencyVisitor, accessType))
    }
  }

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    dependencyVisitor(
      parent,
      AccessPoint(ElementName.fromString(Type.getType(descriptor).getClassName)),
      ANNOTATED
    )
    null
  }

  override def visitField(
    access: Int,
    name: String,
    descriptor: String,
    signature: String,
    value: Object
  ): FieldVisitor = {
    val asmType = Type.getType(descriptor)
    dependencyVisitor(parent, AccessPoint(getElementNameForType(asmType)), COMPOSED)
    visitSignatureWithAccessType(signature, COMPOSED)
    new DependencyFieldVisitor(parent, dependencyVisitor)
  }

  override def visitOuterClass(owner: String, name: String, descriptor: String): Unit = {
    val outer = nameTransformer.transform(owner)
    if (name != null) {
      parent = AccessPoint(outer, AccessPointName.create(name, descriptor))
    } else {
      parent = AccessPoint(outer)
    }
  }

  override def visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String,
    exceptions: Array[String]
  ): MethodVisitor = {
    def pickAccessPointForMethod: AccessPoint = {
      if (parent.attribute != null) {
        parent
      } else {
        parent.copy(attribute = AccessPointName.create(name, descriptor))
      }
    }

    def examineParameters(method: AccessPoint): Unit = {
      val parameters = Type.getArgumentTypes(descriptor)
      parameters.foreach(param =>
        dependencyVisitor(
          method,
          AccessPoint(nameTransformer.transform(getElementNameForType(param).asInternalName)),
          SIGNATURE
        )
      )
    }

    def examineExceptions(method: AccessPoint): Unit = {
      if (exceptions != null) {
        exceptions.foreach(exception =>
          dependencyVisitor(method, AccessPoint(nameTransformer.transform(exception)), SIGNATURE)
        )
      }
    }

    def examineReturnType(method: AccessPoint): Unit = {
      val returnType = Type.getMethodType(descriptor).getReturnType
      dependencyVisitor(
        method,
        AccessPoint(nameTransformer.transform(getElementNameForType(returnType).asInternalName)),
        SIGNATURE
      )
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
  private val OBJECT = ElementName.fromClass(classOf[Object])

  private def filterOutJavaLangObject(delegate: AccessVisitor): AccessVisitor = new AccessVisitor {
    override def apply(source: AccessPoint, dest: AccessPoint, accessType: AccessType): Unit =
      if (!(dest.elementName == OBJECT)) {
        delegate.apply(source, dest, accessType)
      }

    override def newNode(clazz: ElementName): Unit = delegate.newNode(clazz)

    override def newAccessPoint(ap: AccessPoint): Unit = delegate.newAccessPoint(ap)

    override def newEntryPoint(clazz: ElementName): Unit = delegate.newEntryPoint(clazz)
  }
}
