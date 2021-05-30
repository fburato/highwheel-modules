package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot

import java.io.InputStream
import scala.util.{Success, Try}

class ClassLoaderClassPathRoot(classLoader: ClassLoader) extends ClasspathRoot {
  override def getData(elementName: ElementName): Try[Option[InputStream]] =
    Try(Option(classLoader.getResourceAsStream(elementName.asInternalName + ".class")))

  override def classNames: Try[Seq[ElementName]] = Success(Seq())

  override def getResource(name: String): Try[Option[InputStream]] =
    Try(Option(classLoader.getResourceAsStream(name)))
}
