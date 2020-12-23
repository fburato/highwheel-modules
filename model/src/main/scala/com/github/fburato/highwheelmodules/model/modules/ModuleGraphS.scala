package com.github.fburato.highwheelmodules.model.modules

trait ModuleGraphS[T] {
  def findDependency(vertex1: HWModuleS, vertex2: HWModuleS): Option[T]

  def addDependency(dependency: T): Unit

  def addModule(vertex: HWModuleS): Unit

  def modules: Seq[HWModuleS]

  def dependencies(vertex: HWModuleS): Seq[HWModuleS]
}
