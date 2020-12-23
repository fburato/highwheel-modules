package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot

import java.io.InputStream
import scala.util.{Success, Try}

class ClassLoaderClassPathRoot(classLoader: ClassLoader) extends ClasspathRoot {
  override def getData(elementName: ElementNameS): Try[InputStream] =
    Try(classLoader.getResourceAsStream(elementName.asInternalName + ".class"))

  override def classNames: Try[Seq[ElementNameS]] = Success(Seq())

  override def getResource(name: String): Try[InputStream] =
    Try(classLoader.getResourceAsStream(name))
}
