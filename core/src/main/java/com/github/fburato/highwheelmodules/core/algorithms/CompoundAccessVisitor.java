package com.github.fburato.highwheelmodules.core.algorithms;

import org.pitest.highwheel.classpath.AccessVisitor;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.AccessType;
import org.pitest.highwheel.model.ElementName;


public class CompoundAccessVisitor implements AccessVisitor{

  private final AccessVisitor[] accessVisitors;

  public CompoundAccessVisitor(AccessVisitor ... accessVisitors) {
    this.accessVisitors = accessVisitors;
  }

  @Override
  public void apply(AccessPoint accessPoint, AccessPoint accessPoint1, AccessType accessType) {
    for(AccessVisitor av: accessVisitors) {
      av.apply(accessPoint,accessPoint1,accessType);
    }
  }

  @Override
  public void newNode(ElementName elementName) {
    for(AccessVisitor av: accessVisitors) {
      av.newNode(elementName);
    }
  }

  @Override
  public void newAccessPoint(AccessPoint accessPoint) {
    for(AccessVisitor av: accessVisitors) {
      av.newAccessPoint(accessPoint);
    }
  }

  @Override
  public void newEntryPoint(ElementName elementName) {
    for(AccessVisitor av: accessVisitors) {
      av.newEntryPoint(elementName);
    }
  }
}
