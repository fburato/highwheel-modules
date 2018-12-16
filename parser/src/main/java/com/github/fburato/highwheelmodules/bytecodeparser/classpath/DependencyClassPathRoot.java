package com.github.fburato.highwheelmodules.model.bytecodeparser.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

/**
 * Root that provides access to data within a root but will not list its
 * contents
 */
public class DependencyClassPathRoot implements ClasspathRoot {

  private final ClasspathRoot child;

  public InputStream getData(final ElementName name) throws IOException {
    return this.child.getData(name);
  }

  public Collection<ElementName> classNames() {
    return Collections.emptyList();
  }

  public InputStream getResource(final String name) throws IOException {
    return this.child.getResource(name);
  }

  public DependencyClassPathRoot(final ClasspathRoot child) {
    this.child = child;
  }

}
