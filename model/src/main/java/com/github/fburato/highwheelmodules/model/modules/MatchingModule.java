package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public interface MatchingModule {
    boolean contains(ElementName elementName);
}
