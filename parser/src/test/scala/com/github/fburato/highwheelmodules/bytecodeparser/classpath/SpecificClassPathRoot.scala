package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot

import java.io.InputStream
import scala.util.Try

class SpecificClassPathRoot(classes: Array[Class[_]]) extends ClasspathRoot {
  private val internalClassLoaderClassPathRoot = new ClassLoaderClassPathRoot(getClass.getClassLoader)

  private def first3InnerClassesIfPresent(element: ElementNameS): List[ElementNameS] =
    (1 to 3)
      .map(i => ElementNameS.fromString(element.asJavaName + "$" + i))
      .filter(el => internalClassLoaderClassPathRoot.getData(el).get != null)
      .toList

  override def getData(elementName: ElementNameS): Try[InputStream] = internalClassLoaderClassPathRoot.getData(elementName)

  override def classNames: Try[Seq[ElementNameS]] = Try {
    classes
      .map(oneClass => ElementNameS.fromClass(oneClass))
      .flatMap(elementName => elementName :: first3InnerClassesIfPresent(elementName))
      .toIndexedSeq
  }

  override def getResource(name: String): Try[InputStream] = internalClassLoaderClassPathRoot.getResource(name)
}
