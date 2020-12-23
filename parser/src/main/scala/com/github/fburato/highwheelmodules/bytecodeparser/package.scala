package com.github.fburato.highwheelmodules

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import org.objectweb.asm.Type

package object bytecodeparser {
  def getElementNameForType(asmType: Type): ElementName =
    if (asmType.getSort == Type.ARRAY) {
      ElementName.fromString(asmType.getElementType.getClassName)
    } else {
      ElementName.fromString(asmType.getClassName)
    }
}
