package com.github.fburato.highwheelmodules.bytecodeparser;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class DependencyFieldVisitor extends FieldVisitor {

  private final AccessPoint   parent;
  private final AccessVisitor typeReceiver;

  public DependencyFieldVisitor(final AccessPoint owner,
      final AccessVisitor typeReceiver) {
    super(Opcodes.ASM7, null);
    this.typeReceiver = typeReceiver;
    this.parent = owner;
  }
  
  @Override
  public AnnotationVisitor visitAnnotation(final String desc,
      final boolean visible) {
    this.typeReceiver.apply(
        this.parent,
        AccessPoint.create(ElementName.fromString(org.objectweb.asm.Type.getType(
            desc).getClassName())), AccessType.ANNOTATED);
    return null;
  }
  
}