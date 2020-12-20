package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRootS

import java.io.InputStream
import scala.util.{Success, Try}

class ClassLoaderClassPathRootS(classLoader: ClassLoader) extends ClassPathRootDelegator(new InternalClassLoaderClassPathRoot(classLoader))

class InternalClassLoaderClassPathRoot(classLoader: ClassLoader) extends ClasspathRootS {
  override def getData(elementName: ElementName): Try[InputStream] =
    Try(classLoader.getResourceAsStream(elementName.asInternalName + ".class"))

  override def classNames: Try[Seq[ElementName]] = Success(Seq())

  override def getResource(name: String): Try[InputStream] =
    Try(classLoader.getResourceAsStream(name))
}
