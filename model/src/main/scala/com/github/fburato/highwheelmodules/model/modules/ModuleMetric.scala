package com.github.fburato.highwheelmodules.model.modules

trait ModuleMetric {
  def fanInOf(module: HWModule): Option[Int]

  def fanOutOf(module: HWModule): Option[Int]
}
