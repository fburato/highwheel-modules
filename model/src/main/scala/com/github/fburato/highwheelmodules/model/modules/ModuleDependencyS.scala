package com.github.fburato.highwheelmodules.model.modules

case class ModuleDependencyS private(source: HWModuleS, dest: HWModuleS, private var _count: Int = 0) {
  def count: Int = _count

  def incrementCount(): Unit = _count += 1
}
