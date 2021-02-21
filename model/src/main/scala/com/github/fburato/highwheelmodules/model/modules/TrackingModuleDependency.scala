package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint

import scala.collection.mutable

case class TrackingModuleDependency private (
  source: HWModule,
  dest: HWModule,
  private val evidences: mutable.Map[AccessPoint, mutable.Set[AccessPoint]] = mutable.Map()
) {
  private var _evidenceCounter: Int = 0

  def addEvidence(source: AccessPoint, dest: AccessPoint): Unit = {
    evidences
      .getOrElseUpdate(source, new mutable.HashSet[AccessPoint]())
      .add(dest)
    _evidenceCounter += 1
  }

  def evidenceCounter: Int = _evidenceCounter

  def sources: Set[AccessPoint] = evidences.keySet.toSet

  def destinations: Set[AccessPoint] = evidences.values.flatten.toSet

  def destinationsFromSource(source: AccessPoint): Set[AccessPoint] =
    evidences
      .get(source)
      .map(_.toSet)
      .getOrElse(Set())
}
