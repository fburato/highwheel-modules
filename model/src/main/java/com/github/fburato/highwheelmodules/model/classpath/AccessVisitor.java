package com.github.fburato.highwheelmodules.model.classpath;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public interface AccessVisitor {

    void apply(AccessPoint source, AccessPoint dest, AccessType type);

    void newNode(ElementName clazz);

    void newAccessPoint(AccessPoint ap);

    void newEntryPoint(ElementName clazz);

}
