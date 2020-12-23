package com.github.fburato.highwheelmodules.bytecodeparser

import com.example._
import com.example.annotated._
import com.example.classliterals.{HasFieldOfTypeClassFoo, MethodAccessFooClassLiteral, StoresFooArrayClassLiteralAsField, StoresFooClassLiteralAsField}
import com.example.generics.{BoundedByFoo, HasCollectionOfFooParameter, ImplementsGenericisedInterface, ReturnsCollectionOfFoo}
import com.example.innerclasses.CallsMethodFromFooWithinInnerClass
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.SpecificClassPathRoot
import com.github.fburato.highwheelmodules.model.bytecode._
import com.github.fburato.highwheelmodules.model.classpath.{AccessVisitor, ClassParser}
import org.mockito.scalatest.MockitoSugar
import org.objectweb.asm.Type
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class ClassPathParserSystemSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val accessVisitor = mock[AccessVisitor]

  def onlyExampleDotCom: ClassParser = new ClassPathParser(el => el.asJavaName.startsWith("com.example"))

  def parseClasses(classes: Class[_]*): Unit =
    onlyExampleDotCom.parse(new SpecificClassPathRoot(classes.toArray), accessVisitor).get

  def accessType(clazz: Class[_]): AccessPoint = AccessPoint(ElementName.fromClass(clazz))


  def checkApply(source: Class[_], dest: Class[_], aType: AccessType): Unit =
    verify(accessVisitor).apply(accessType(source), accessType(dest), aType)

  def checkApply(source: Class[_], accessPointName: AccessPointName, dest: Class[_], aType: AccessType): Unit =
    verify(accessVisitor).apply(AccessPoint(ElementName.fromClass(source), accessPointName), accessType(dest), aType)

  def method(name: String, retType: Class[_]): AccessPointName =
    AccessPointName(name, Type.getMethodDescriptor(Type.getType(retType)))

  def method(name: String, descriptor: String): AccessPointName =
    AccessPointName(name, descriptor)

  def methodWithParameter(name: String, paramType: Class[_]): AccessPointName =
    AccessPointName(name, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(paramType)))

  def checkApply(source: Class[_], dest: Class[_], accessPointName: AccessPointName, aType: AccessType): Unit =
    verify(accessVisitor).apply(accessType(source), AccessPoint(ElementName.fromClass(dest), accessPointName), aType)

  def checkApply(source: Class[_], sourceAccessPointName: AccessPointName, dest: Class[_], destAccessPointName: AccessPointName, aType: AccessType): Unit =
    verify(accessVisitor).apply(AccessPoint(ElementName.fromClass(source), sourceAccessPointName), AccessPoint(ElementName.fromClass(dest), destAccessPointName), aType)

  "parse" should {
    "detect an inheritance dependency when one class extends another" in {
      parseClasses(classOf[ExtendsFoo], classOf[Foo])

      checkApply(classOf[ExtendsFoo], classOf[Foo], INHERITANCE)
    }

    "detect an implements dependency when class implements interface" in {
      parseClasses(classOf[ImplementsAnInterface], classOf[AnInterface])

      checkApply(classOf[ImplementsAnInterface], classOf[AnInterface], IMPLEMENTS)
    }

    "detect a composition dependency when class includes another" in {
      parseClasses(classOf[HasFooAsMember], classOf[Foo])

      checkApply(classOf[HasFooAsMember], classOf[Foo], COMPOSED)
    }

    "detect a composition dependency when class includes array field" in {
      parseClasses(classOf[HasArrayOfFooAsMember], classOf[Foo])

      checkApply(classOf[HasArrayOfFooAsMember], classOf[Foo], COMPOSED)
    }

    "detect signature dependency when method returns a type" in {
      parseClasses(classOf[ReturnsAFoo], classOf[Foo])

      checkApply(classOf[ReturnsAFoo], method("foo", classOf[Foo]), classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when class returns an array" in {
      parseClasses(classOf[ReturnsArrayOfFoo], classOf[Foo])

      checkApply(classOf[ReturnsArrayOfFoo], method("foo", classOf[Array[Foo]]), classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when method has parameter of another type" in {
      parseClasses(classOf[HasFooAsParameter], classOf[Foo])

      checkApply(classOf[HasFooAsParameter], methodWithParameter("foo", classOf[Foo]), classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when method has parameter of array of another type" in {
      parseClasses(classOf[HasFooArrayAsParameter], classOf[Foo])

      checkApply(classOf[HasFooArrayAsParameter], methodWithParameter("foo", classOf[Array[Foo]]), classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when method declares an exception in throws" in {
      parseClasses(classOf[DeclaresAnException], classOf[AnException])

      checkApply(classOf[DeclaresAnException], method("foo", "()V"), classOf[AnException], SIGNATURE)
    }

    "detect uses dependency when method code uses a type via constructor" in {
      parseClasses(classOf[ConstructsAFoo], classOf[Foo])

      checkApply(classOf[ConstructsAFoo], method("foo", classOf[Object]), classOf[Foo], method("(init)", "()V"), USES)
    }

    "detect uses dependency when method code calls a method of a type" in {
      parseClasses(classOf[CallsFooMethod], classOf[Foo])

      checkApply(classOf[CallsFooMethod], method("foo", classOf[Object]), classOf[Foo], method("aMethod", classOf[Object]), USES)
    }

    "detect annotation dependency when type annotated at class level" in {
      parseClasses(classOf[AnnotatedAtClassLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtClassLevel], classOf[AnAnnotation], ANNOTATED)
    }

    "detect annotation dependency when type annotated at method level" in {
      parseClasses(classOf[AnnotatedAtMethodLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtMethodLevel], method("foo", "()V"), classOf[AnAnnotation], ANNOTATED)
    }

    "detect annotation dependency when type annotated at parameter level" in {
      parseClasses(classOf[AnnotatedAtParameterLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtParameterLevel], method("foo", "(I)V"), classOf[AnAnnotation], ANNOTATED)
    }

    "detect annotation dependency when type annotation at field level" in {
      parseClasses(classOf[AnnotatedAtFieldLevel], classOf[AnAnnotation])

      checkApply(classOf[AnnotatedAtFieldLevel], classOf[AnAnnotation], ANNOTATED)
    }

    "not detect annotation dependency when type annotated at variable level" in {
      parseClasses(classOf[AnnotatedAtVariableLevel], classOf[AnAnnotation])

      verify(accessVisitor, never).apply(accessType(classOf[AnnotatedAtVariableLevel]), accessType(classOf[AnAnnotation]), ANNOTATED)
    }

    "detect uses dependency when nested type calls parent class method" in {
      parseClasses(classOf[CallsMethodFromFooWithinInnerClass], classOf[Foo])

      checkApply(classOf[CallsMethodFromFooWithinInnerClass], method("foo", "()V"), classOf[Foo], method("aMethod", classOf[Object]), USES)
    }

    "detect uses dependency when type writes to class field" in {
      parseClasses(classOf[UsesFieldOnFoo], classOf[Foo])

      checkApply(classOf[UsesFieldOnFoo], method("foo", "()V"), classOf[Foo], method("aField", "I"), USES)
    }

    "detect uses dependency when type stores class literal as field" in {
      parseClasses(classOf[StoresFooClassLiteralAsField], classOf[Foo])

      checkApply(classOf[StoresFooClassLiteralAsField], method("(init)", "()V"), classOf[Foo], USES)
    }

    "detect uses dependency when type stores class array literal in method" in {
      parseClasses(classOf[StoresFooArrayClassLiteralAsField], classOf[Foo])

      checkApply(classOf[StoresFooArrayClassLiteralAsField], method("(init)", "()V"), classOf[Foo], USES)
    }

    "detect uses dependency when type uses class literal in method" in {
      parseClasses(classOf[MethodAccessFooClassLiteral], classOf[Foo])

      checkApply(classOf[MethodAccessFooClassLiteral], method("foo", classOf[Class[_]]), classOf[Foo], USES)
    }

    "detect composition dependency when type declare field of another class" in {
      parseClasses(classOf[HasFieldOfTypeClassFoo], classOf[Foo])

      checkApply(classOf[HasFieldOfTypeClassFoo], classOf[Foo], COMPOSED)
    }

    "detect signature dependency when type implements interface parametrised by another class" in {
      parseClasses(classOf[ImplementsGenericisedInterface], classOf[Foo])

      checkApply(classOf[ImplementsGenericisedInterface], classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when type returns generic type instantiated to another class" in {
      parseClasses(classOf[ReturnsCollectionOfFoo], classOf[Foo])

      checkApply(classOf[ReturnsCollectionOfFoo], classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when type uses a generic type instantiated to another class" in {
      parseClasses(classOf[HasCollectionOfFooParameter], classOf[Foo])

      checkApply(classOf[HasCollectionOfFooParameter], classOf[Foo], SIGNATURE)
    }

    "detect signature dependency when generic parameter bounded by another class" in {
      parseClasses(classOf[BoundedByFoo[_]], classOf[Foo])

      checkApply(classOf[BoundedByFoo[_]], classOf[Foo], SIGNATURE)
    }

    "detect unconnected types" in {
      parseClasses(classOf[Unconnected])

      verify(accessVisitor).newNode(ElementName.fromClass(classOf[Unconnected]))
    }

    "detect unconnected methods" in {
      parseClasses(classOf[Foo])

      verify(accessVisitor).newAccessPoint(AccessPoint(ElementName.fromClass(classOf[Foo]), method("aMethod", "()Ljava/lang/Object;")))
    }

    "detect entry point in type with main method" in {
      parseClasses(classOf[HasMainMethod])

      verify(accessVisitor).newEntryPoint(ElementName.fromClass(classOf[HasMainMethod]))
    }

    "not detect entry point in type without main method" in {
      parseClasses(classOf[Foo])

      verify(accessVisitor, never).newEntryPoint(any)
    }

    "detect uses dependency when method instantiates an anonymous interface instance" in {
      parseClasses(classOf[UsesAnInterfaceInMethod], classOf[AnInterface])

      checkApply(classOf[UsesAnInterfaceInMethod], method("foo", "()V"), classOf[AnInterface], USES)
    }

    "detect uses dependency when type uses a method reference" in {
      parseClasses(classOf[UsesMethodReference], classOf[Foo])

      checkApply(classOf[UsesMethodReference], method("foo", "()V"), classOf[Foo], method("aMethod", "()Ljava/lang/Object;"), USES)
    }
  }

}
