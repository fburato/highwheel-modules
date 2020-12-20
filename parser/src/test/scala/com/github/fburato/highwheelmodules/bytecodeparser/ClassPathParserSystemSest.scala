package com.github.fburato.highwheelmodules.bytecodeparser

import com.example._
import com.example.annotated._
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.SpecificClassPathRoot
import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessPointName, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.{AccessVisitor, ClassParser}
import org.mockito.scalatest.MockitoSugar
import org.objectweb.asm.Type
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class ClassPathParserSystemSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val accessVisitor = mock[AccessVisitor]

  def onlyExampleDotCom: ClassParser = new ClassPathParser(el => el.asJavaName().startsWith("com.example"))

  def parseClasses(classes: Class[_]*): Unit =
    onlyExampleDotCom.parse(new SpecificClassPathRoot(classes.toArray), accessVisitor).get

  def accessType(clazz: Class[_]): AccessPoint = AccessPoint.create(ElementName.fromClass(clazz))


  def checkApply(source: Class[_], dest: Class[_], aType: AccessType): Unit =
    verify(accessVisitor).apply(accessType(source), accessType(dest), aType)

  def checkApply(source: Class[_], accessPointName: AccessPointName, dest: Class[_], aType: AccessType): Unit =
    verify(accessVisitor).apply(AccessPoint.create(ElementName.fromClass(source), accessPointName), accessType(dest), aType)

  def method(name: String, retType: Class[_]): AccessPointName =
    AccessPointName.create(name, Type.getMethodDescriptor(Type.getType(retType)))

  def method(name: String, descriptor: String): AccessPointName =
    AccessPointName.create(name, descriptor)

  def methodWithParameter(name: String, paramType: Class[_]): AccessPointName =
    AccessPointName.create(name, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(paramType)))

  def checkApply(source: Class[_], dest: Class[_], accessPointName: AccessPointName, aType: AccessType): Unit =
    verify(accessVisitor).apply(accessType(source), AccessPoint.create(ElementName.fromClass(dest), accessPointName), aType)

  def checkApply(source: Class[_], sourceAccessPointName: AccessPointName, dest: Class[_], destAccessPointName: AccessPointName, aType: AccessType): Unit =
    verify(accessVisitor).apply(AccessPoint.create(ElementName.fromClass(source), sourceAccessPointName), AccessPoint.create(ElementName.fromClass(dest), destAccessPointName), aType)

  "parse" should {
    "detect an inheritance dependency when one class extends another" in {
      parseClasses(classOf[ExtendsFoo], classOf[Foo])

      checkApply(classOf[ExtendsFoo], classOf[Foo], AccessType.INHERITANCE)
    }

    "detect an implements dependency when class implements interface" in {
      parseClasses(classOf[ImplementsAnInterface], classOf[AnInterface])

      checkApply(classOf[ImplementsAnInterface], classOf[AnInterface], AccessType.IMPLEMENTS)
    }

    "detect a composition dependency when class includes another" in {
      parseClasses(classOf[HasFooAsMember], classOf[Foo])

      checkApply(classOf[HasFooAsMember], classOf[Foo], AccessType.COMPOSED)
    }

    "detect a composition dependency when class includes array field" in {
      parseClasses(classOf[HasArrayOfFooAsMember], classOf[Foo])

      checkApply(classOf[HasArrayOfFooAsMember], classOf[Foo], AccessType.COMPOSED)
    }

    "detect signature dependency when method returns a type" in {
      parseClasses(classOf[ReturnsAFoo], classOf[Foo])

      checkApply(classOf[ReturnsAFoo], method("foo", classOf[Foo]), classOf[Foo], AccessType.SIGNATURE)
    }

    "detect signature dependency when class returns an array" in {
      parseClasses(classOf[ReturnsArrayOfFoo], classOf[Foo])

      checkApply(classOf[ReturnsArrayOfFoo], method("foo", classOf[Array[Foo]]), classOf[Foo], AccessType.SIGNATURE)
    }

    "detect signature dependency when method has parameter of another type" in {
      parseClasses(classOf[HasFooAsParameter], classOf[Foo])

      checkApply(classOf[HasFooAsParameter], methodWithParameter("foo", classOf[Foo]), classOf[Foo], AccessType.SIGNATURE)
    }

    "detect signature dependency when method has parameter of array of another type" in {
      parseClasses(classOf[HasFooArrayAsParameter], classOf[Foo])

      checkApply(classOf[HasFooArrayAsParameter], methodWithParameter("foo", classOf[Array[Foo]]), classOf[Foo], AccessType.SIGNATURE)
    }

    "detect signature dependency when method declares an exception in throws" in {
      parseClasses(classOf[DeclaresAnException], classOf[AnException])

      checkApply(classOf[DeclaresAnException], method("foo", "()V"), classOf[AnException], AccessType.SIGNATURE)
    }

    "detect uses dependency when method code uses a type via constructor" in {
      parseClasses(classOf[ConstructsAFoo], classOf[Foo])

      checkApply(classOf[ConstructsAFoo], method("foo", classOf[Object]), classOf[Foo], method("<init>", "()V"), AccessType.USES)
    }

    "detect uses dependency when method code calls a method of a type" in {
      parseClasses(classOf[CallsFooMethod], classOf[Foo])

      checkApply(classOf[CallsFooMethod], method("foo", classOf[Object]), classOf[Foo], method("aMethod", classOf[Object]), AccessType.USES)
    }

    "detect annotation dependency when type annotated at class level" in {
      parseClasses(classOf[AnnotatedAtClassLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtClassLevel], classOf[AnAnnotation], AccessType.ANNOTATED)
    }

    "detect annotation dependency when type annotated at method level" in {
      parseClasses(classOf[AnnotatedAtMethodLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtMethodLevel], method("foo", "()V"), classOf[AnAnnotation], AccessType.ANNOTATED)
    }

    "detect annotation dependency when type annotated at parameter level" in {
      parseClasses(classOf[AnnotatedAtParameterLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtParameterLevel], method("foo", "(I)V"), classOf[AnAnnotation], AccessType.ANNOTATED)
    }

    "detect annotation dependency when type annotation at field level" in {
      parseClasses(classOf[AnnotatedAtFieldLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtFieldLevel], classOf[AnAnnotation], AccessType.ANNOTATED)
    }

    "not detect annotation dependency when type annotated at variable level" in {
      parseClasses(classOf[AnnotatedAtVariableLevel], classOf[AnAnnotation])

      verify(accessVisitor, never).apply(accessType(classOf[AnnotatedAtVariableLevel]), accessType(classOf[AnAnnotation]), AccessType.ANNOTATED)
    }
  }

}
