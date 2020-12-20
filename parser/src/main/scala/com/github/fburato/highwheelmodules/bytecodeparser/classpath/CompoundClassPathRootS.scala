package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.{ClasspathRoot, ClasspathRootS}
import com.github.fburato.highwheelmodules.utils.TryUtils._

import java.io.InputStream
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

class CompoundClassPathRootS(roots: java.util.List[ClasspathRoot]) extends ClassPathRootDelegator(
  new InternalClassPathRoot(roots.asScala.toSeq.map(CompoundClassPathRootS.fromJava)))

object CompoundClassPathRootS {
  def fromJava(cpr: ClasspathRoot): ClasspathRootS = new ClasspathRootS {
    override def getData(elementName: ElementName): Try[InputStream] = Try(cpr.getData(elementName))

    override def classNames: Try[Seq[ElementName]] = Try(cpr.classNames.asScala.toSeq)

    override def getResource(name: String): Try[InputStream] = Try(cpr.getResource(name))
  }
}

class InternalClassPathRoot(roots: Seq[ClasspathRootS]) extends ClasspathRootS {
  override def getData(elementName: ElementName): Try[InputStream] =
    sequence(roots.map(cpr => cpr.getData(elementName)))
      .map(getFirstNonNullOrNull)

  private def getFirstNonNullOrNull(streams: Seq[InputStream]): InputStream = streams
    .filter(stream => stream != null)
    .collectFirst(stream => stream)
    .orNull

  override def classNames: Try[Seq[ElementName]] =
    sequence(roots.map(cpr => cpr.classNames))
      .map(result => result.flatten)

  override def getResource(name: String): Try[InputStream] =
    sequence(roots.map(cpr => cpr.getResource(name)))
      .map(getFirstNonNullOrNull)
}