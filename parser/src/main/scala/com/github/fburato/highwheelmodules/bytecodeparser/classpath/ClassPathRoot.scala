package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot
import com.github.fburato.highwheelmodules.utils.TryUtils._

import java.io.InputStream
import scala.util.Try

class ClassPathRoot(roots: Seq[ClasspathRoot]) extends ClasspathRoot {
  override def getData(elementName: ElementNameS): Try[InputStream] =
    sequence(roots.map(cpr => cpr.getData(elementName)))
      .map(getFirstNonNullOrNull)

  private def getFirstNonNullOrNull(streams: Seq[InputStream]): InputStream = streams
    .filter(stream => stream != null)
    .collectFirst(stream => stream)
    .orNull

  override def classNames: Try[Seq[ElementNameS]] =
    sequence(roots.map(cpr => cpr.classNames))
      .map(result => result.flatten)

  override def getResource(name: String): Try[InputStream] =
    sequence(roots.map(cpr => cpr.getResource(name)))
      .map(getFirstNonNullOrNull)
}