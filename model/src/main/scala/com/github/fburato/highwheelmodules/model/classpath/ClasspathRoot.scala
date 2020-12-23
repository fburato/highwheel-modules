package com.github.fburato.highwheelmodules.model.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS

import java.io.InputStream
import scala.util.Try


trait ClasspathRoot {

  def getData(elementName: ElementNameS): Try[InputStream]

  def classNames: Try[Seq[ElementNameS]]

  def getResource(name: String): Try[InputStream]
}
