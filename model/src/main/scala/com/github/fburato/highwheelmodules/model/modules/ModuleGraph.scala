package com.github.fburato.highwheelmodules.model.modules

trait ModuleGraph[T] {
  def findDependency(vertex1: HWModule, vertex2: HWModule): Option[T]

  def addDependency(dependency: T): Unit

  def addModule(vertex: HWModule): Unit

  def modules: Seq[HWModule]

  def dependencies(vertex: HWModule): Seq[HWModule]
}
