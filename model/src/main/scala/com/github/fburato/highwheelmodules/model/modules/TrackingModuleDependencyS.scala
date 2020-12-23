package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.AccessPointS

import scala.collection.mutable

case class TrackingModuleDependencyS private(
                                              source: HWModuleS,
                                              dest: HWModuleS,
                                              private val evidences: mutable.Map[AccessPointS, mutable.Set[AccessPointS]] = mutable.Map()
                                            ) {
  private var _evidenceCounter: Int = 0

  def addEvidence(source: AccessPointS, dest: AccessPointS): Unit = {
    evidences.getOrElseUpdate(source, new mutable.HashSet[AccessPointS]())
      .add(dest)
    _evidenceCounter += 1
  }

  def evidenceCounter: Int = _evidenceCounter

  def sources: Set[AccessPointS] = evidences.keySet.toSet

  def destinations: Set[AccessPointS] = evidences.values.flatten.toSet

  def destinationsFromSource(source: AccessPointS): Set[AccessPointS] =
    evidences.get(source)
      .map(_.toSet)
      .getOrElse(Set())
}
