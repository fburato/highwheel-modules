package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.Arrays;
import java.util.List;

public class CompoundAccessVisitor implements AccessVisitor {

    private final List<AccessVisitor> accessVisitors;

    public CompoundAccessVisitor(AccessVisitor... accessVisitors) {
        this.accessVisitors = Arrays.asList(accessVisitors);
    }

    public CompoundAccessVisitor(List<AccessVisitor> accessVisitors) {
        this.accessVisitors = accessVisitors;
    }

    @Override
    public void apply(AccessPoint accessPoint, AccessPoint accessPoint1, AccessType accessType) {
        for (AccessVisitor av : accessVisitors) {
            av.apply(accessPoint, accessPoint1, accessType);
        }
    }

    @Override
    public void newNode(ElementName elementName) {
        for (AccessVisitor av : accessVisitors) {
            av.newNode(elementName);
        }
    }

    @Override
    public void newAccessPoint(AccessPoint accessPoint) {
        for (AccessVisitor av : accessVisitors) {
            av.newAccessPoint(accessPoint);
        }
    }

    @Override
    public void newEntryPoint(ElementName elementName) {
        for (AccessVisitor av : accessVisitors) {
            av.newEntryPoint(elementName);
        }
    }
}
