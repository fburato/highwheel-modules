package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.AccessPointS

case class EvidenceModuleDependencyS(sourceModule: HWModuleS, destModule: HWModuleS, source: AccessPointS, dest: AccessPointS)
