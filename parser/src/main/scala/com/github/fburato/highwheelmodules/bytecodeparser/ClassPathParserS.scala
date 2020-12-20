package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath._
import com.github.fburato.highwheelmodules.utils.TryUtils._
import org.objectweb.asm.ClassReader

import java.io.InputStream
import java.util.function.Predicate
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Try, Using}

class ClassPathParserS(predicate: Predicate[ElementName]) extends ClassParser {
  private val delegate = new InternalClassPathParser(el => predicate.test(el))

  override def parse(cp: ClasspathRoot, v: AccessVisitor): Unit =
    delegate.parse(fromJava(cp), fromJava(v))

  private def fromJava(cpr: ClasspathRoot): ClasspathRootS = new ClasspathRootS {
    override def getData(elementName: ElementName): Try[InputStream] = Try(cpr.getData(elementName))

    override def classNames: Try[Seq[ElementName]] = Try(cpr.classNames.asScala.toSeq)

    override def getResource(name: String): Try[InputStream] = Try(cpr.getResource(name))
  }

  private def fromJava(av: AccessVisitor): AccessVisitorS = new AccessVisitorS {
    override def apply(source: AccessPoint, dest: AccessPoint, accessType: AccessType): Unit = av.apply(source, dest, accessType)

    override def newNode(clazz: ElementName): Unit = av.newNode(clazz)

    override def newAccessPoint(ap: AccessPoint): Unit = av.newAccessPoint(ap)

    override def newEntryPoint(clazz: ElementName): Unit = av.newEntryPoint(clazz)
  }
}

class InternalClassPathParser(filter: ElementName => Boolean) extends ClassParserS {
  private val nameTransformer: NameTransformer = s =>
    if (s.contains("$")) {
      ElementName.fromString(s.substring(0, s.indexOf('$')))
    } else {
      ElementName.fromString(s)
    }

  override def parse(cpr: ClasspathRootS, accessVisitor: AccessVisitorS): Try[Unit] = {
    val dependencyClassVisitor = new DependencyClassVisitor(null, new FilteringDecorator(accessVisitor, filter), nameTransformer)

    def readAndVisitClassStream(is: InputStream): Try[Unit] =
      for {
        classReader <- Try(new ClassReader(is))
        _ <- Try(classReader.accept(dependencyClassVisitor, 0))
      } yield ()

    def parseElement(elementName: ElementName): Try[Unit] =
      for {
        stream <- cpr.getData(elementName)
        _ <- Using(stream)(readAndVisitClassStream)
      } yield ()

    for {
      classes <- cpr.classNames
      _ <- sequence(classes.filter(filter).map(parseElement))
    } yield ()
  }
}
