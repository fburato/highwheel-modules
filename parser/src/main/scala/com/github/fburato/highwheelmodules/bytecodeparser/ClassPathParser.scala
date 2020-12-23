package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.classpath._
import com.github.fburato.highwheelmodules.utils.TryUtils._
import org.objectweb.asm.ClassReader

import java.io.InputStream
import java.util.function.Predicate
import scala.util.{Try, Using}

class ClassPathParser(filter: ElementNameS => Boolean) extends ClassParser {

  def this(predicate: Predicate[ElementNameS]) = this(el => predicate.test(el))

  private val nameTransformer: NameTransformer = s =>
    if (s.contains("$")) {
      ElementNameS.fromString(s.substring(0, s.indexOf('$')))
    } else {
      ElementNameS.fromString(s)
    }

  override def parse(cpr: ClasspathRoot, accessVisitor: AccessVisitor): Try[Unit] = {
    val dependencyClassVisitor = new DependencyClassVisitor(null, new FilteringDecorator(accessVisitor, filter), nameTransformer)

    def readAndVisitClassStream(is: InputStream): Try[Unit] =
      for {
        classReader <- Try(new ClassReader(is))
        _ <- Try(classReader.accept(dependencyClassVisitor, 0))
      } yield ()

    def parseElement(elementName: ElementNameS): Try[Unit] =
      for {
        stream <- cpr.getData(elementName)
        compute <- Using(stream)(readAndVisitClassStream)
        _ <- compute
      } yield ()

    for {
      classes <- cpr.classNames
      _ <- sequence(classes.filter(filter).map(parseElement))
    } yield ()
  }
}
