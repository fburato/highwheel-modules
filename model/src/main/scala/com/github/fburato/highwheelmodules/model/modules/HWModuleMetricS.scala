package com.github.fburato.highwheelmodules.model.modules

trait HWModuleMetricS {
  def fanInOf(module: HWModuleS): Option[Int]

  def fanOutOf(module: HWModuleS): Option[Int]
}
