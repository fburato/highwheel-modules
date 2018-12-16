package com.github.fburato.highwheelmodules.bytecodeparser;

import org.objectweb.asm.Type;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class NameUtil {
    
  static ElementName getElementNameForType(final org.objectweb.asm.Type type) {
    if (type.getSort() == Type.ARRAY) {
      return ElementName.fromString(type.getElementType().getClassName());
    }
    return ElementName.fromString(type.getClassName());
  }
}
