package com.github.fburato.highwheelmodules.bytecodeparser;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public interface NameTransformer {

    ElementName transform(String name);

}
