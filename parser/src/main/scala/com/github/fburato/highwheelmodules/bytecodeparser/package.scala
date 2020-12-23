package com.github.fburato.highwheelmodules

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import org.objectweb.asm.Type

package object bytecodeparser {
  def getElementNameForType(asmType: Type): ElementNameS =
    if (asmType.getSort == Type.ARRAY) {
      ElementNameS.fromString(asmType.getElementType.getClassName)
    } else {
      ElementNameS.fromString(asmType.getClassName)
    }
}
