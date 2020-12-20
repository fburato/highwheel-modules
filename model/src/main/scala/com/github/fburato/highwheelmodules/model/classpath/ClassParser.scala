package com.github.fburato.highwheelmodules.model.classpath

import scala.util.Try

trait ClassParser {
  def parse(cpr: ClasspathRoot, accessVisitor: AccessVisitor): Try[Unit]
}
