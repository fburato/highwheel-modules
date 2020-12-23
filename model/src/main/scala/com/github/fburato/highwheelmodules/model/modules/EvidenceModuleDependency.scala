package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint

case class EvidenceModuleDependency(sourceModule: HWModule, destModule: HWModule, source: AccessPoint, dest: AccessPoint)
