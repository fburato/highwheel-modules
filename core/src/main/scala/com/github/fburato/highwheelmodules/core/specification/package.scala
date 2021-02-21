package com.github.fburato.highwheelmodules.core

package object specification {

  case class ModuleDefinition(moduleName: String, moduleRegex: List[String])

  sealed trait Rule

  case class ChainDependencyRule(moduleNameChain: List[String]) extends Rule

  case class NoDependentRule(left: String, right: String) extends Rule

  case class OneToManyRule(one: String, many: List[String]) extends Rule

  case class ManyToOneRule(many: List[String], one: String) extends Rule

  case class Definition(
    prefix: Option[String],
    whitelist: Option[List[String]],
    blacklist: Option[List[String]],
    mode: Option[String],
    moduleDefinitions: List[ModuleDefinition],
    rules: List[Rule]
  )

}
