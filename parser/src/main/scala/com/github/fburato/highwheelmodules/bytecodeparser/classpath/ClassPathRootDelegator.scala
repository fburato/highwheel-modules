package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.{ClasspathRoot, ClasspathRootS}

import java.io.InputStream
import java.util
import scala.jdk.CollectionConverters.IterableHasAsJava

class ClassPathRootDelegator(delegate: ClasspathRootS) extends ClasspathRoot {

  override def getData(name: ElementName): InputStream = delegate.getData(name).get

  override def classNames(): util.Collection[ElementName] = delegate.classNames.get.asJavaCollection

  override def getResource(name: String): InputStream = delegate.getResource(name).get
}
