package com.github.fburato.highwheelmodules.model.classpath

import scala.util.Try

trait ClassParserS {
  def parse(cpr: ClasspathRootS, accessVisitor: AccessVisitorS): Try[Unit]
}
