package com.github.fburato.highwheelmodules.model.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName

import java.io.InputStream
import scala.util.Try


trait ClasspathRootS {

  def getData(elementName: ElementName): Try[InputStream]

  def classNames: Try[Seq[ElementName]]

  def getResource(name: String): Try[InputStream]
}
