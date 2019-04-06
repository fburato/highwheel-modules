package com.github.fburato.highwheelmodules.bytecodeparser;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class CollapseInnerClassesNameTransformer implements NameTransformer {

    public ElementName transform(String clazz) {
        if (clazz.contains("$")) {
            return ElementName.fromString(clazz.substring(0, clazz.indexOf('$')));
        }
        return ElementName.fromString(clazz);

    }

}
