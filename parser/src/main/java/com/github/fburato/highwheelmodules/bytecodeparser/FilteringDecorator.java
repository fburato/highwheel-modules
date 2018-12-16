package com.github.fburato.highwheelmodules.model.bytecodeparser;

import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.function.Predicate;

/**
 * Passes calls through to wrapped child only if
 * they match the supplied filter
 */
class FilteringDecorator implements AccessVisitor {

  private final AccessVisitor child;
  private final Predicate<ElementName> filter;

  public FilteringDecorator(final AccessVisitor child, final Predicate<ElementName> filter) {
    this.child = child;
    this.filter = filter;
  }

  @Override
  public void apply(final AccessPoint source, final AccessPoint dest,
      final AccessType type) {
    if (this.filter.test(dest.getElementName()) && this.filter.test(source.getElementName())) {
      this.child.apply(source, dest, type);
    }
  }

  @Override
  public void newNode(final ElementName clazz) {
    if (this.filter.test(clazz)) {
      this.child.newNode(clazz);
    }
  }

  @Override
  public void newEntryPoint(ElementName clazz) {
    if (this.filter.test(clazz)) {
      this.child.newEntryPoint(clazz);
    }
  }

  @Override
  public void newAccessPoint(AccessPoint ap) {
    this.child.newAccessPoint(ap);
  }

}
