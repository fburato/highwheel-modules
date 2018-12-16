package com.github.fburato.highwheelmodules.bytecodeparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

import org.objectweb.asm.ClassReader;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.classpath.ClassParser;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class ClassPathParser implements ClassParser {

  private final Predicate<ElementName> filter;
  private final NameTransformer nameTransformer;
  
  public ClassPathParser(final Predicate<ElementName> filter) {
    this(filter, new CollapseInnerClassesNameTransformer());
  }

  public ClassPathParser(final Predicate<ElementName> filter, final NameTransformer nameTransformer) {
    this.filter = filter;
    this.nameTransformer = nameTransformer;
  }

  public void parse(final ClasspathRoot classes, final AccessVisitor v) throws IOException {

    for (final ElementName each : classes.classNames()) {
      if (this.filter.test(each)) {
        parseClass(classes, v, each);
      }
    }

  }

  private void parseClass(final ClasspathRoot cp, final AccessVisitor dv,
      final ElementName each) throws IOException {
    final InputStream is = cp.getData(each);
    try {
      final ClassReader reader = new ClassReader(is);
      final DependencyClassVisitor cv = new DependencyClassVisitor(null,
          new FilteringDecorator(dv, this.filter), nameTransformer);
      reader.accept(cv, 0);
    } finally {
      is.close();
    }

  }

}
