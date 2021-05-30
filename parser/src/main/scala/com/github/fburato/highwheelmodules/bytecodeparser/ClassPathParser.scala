package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath._
import com.github.fburato.highwheelmodules.utils.TryUtils._
import org.objectweb.asm.ClassReader

import java.io.InputStream
import scala.util.{Success, Try, Using}

class ClassPathParser(filter: ElementName => Boolean) extends ClassParser {

  private val nameTransformer: NameTransformer = s =>
    if (s.contains("$")) {
      ElementName.fromString(s.substring(0, s.indexOf('$')))
    } else {
      ElementName.fromString(s)
    }

  override def parse(cpr: ClasspathRoot, accessVisitor: AccessVisitor): Try[Unit] = {
    val dependencyClassVisitor = new DependencyClassVisitor(
      null,
      new FilteringDecorator(accessVisitor, filter),
      nameTransformer
    )

    def readAndVisitClassStream(is: InputStream): Try[Unit] =
      for {
        classReader <- Try(new ClassReader(is))
        _ <- Try(classReader.accept(dependencyClassVisitor, 0))
      } yield ()

    def parseElement(elementName: ElementName): Try[Unit] =
      for {
        maybeStream <- cpr.getData(elementName)
        compute <- maybeStream.map(stream => Using(stream)(readAndVisitClassStream)).getOrElse(Success(Success()))
        _ <- compute
      } yield ()

    for {
      classes <- cpr.classNames
      _ <- sequence(classes.filter(filter).map(parseElement))
    } yield ()
  }
}
