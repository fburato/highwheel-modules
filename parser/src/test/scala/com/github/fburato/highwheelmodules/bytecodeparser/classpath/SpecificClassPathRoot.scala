package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot

import java.io.InputStream
import scala.util.Try

class SpecificClassPathRoot(classes: Array[Class[_]]) extends ClasspathRoot {
  private val internalClassLoaderClassPathRoot = new ClassLoaderClassPathRoot(getClass.getClassLoader)

  private def first3InnerClassesIfPresent(element: ElementName): Seq[ElementName] =
    (1 to 3)
      .map(i => ElementName.fromString(element.asJavaName() + "$" + i))
      .filter(el => internalClassLoaderClassPathRoot.getData(el).get != null)

  override def getData(elementName: ElementName): Try[InputStream] = internalClassLoaderClassPathRoot.getData(elementName)

  override def classNames: Try[Seq[ElementName]] = Try {
    classes
      .map(oneClass => ElementName.fromClass(oneClass))
      .flatMap(elementName => Seq(elementName) ++ first3InnerClassesIfPresent(elementName))
  }

  override def getResource(name: String): Try[InputStream] = internalClassLoaderClassPathRoot.getResource(name)
}
