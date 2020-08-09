package com.github.fburato.highwheelmodules.bytecodeparser;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class DependencySignatureVisitor extends SignatureVisitor {

    private final AccessPoint parent;
    private final AccessVisitor typeReceiver;
    private final AccessType type;

    public DependencySignatureVisitor(final AccessPoint owner, final AccessVisitor typeReceiver, AccessType type) {
        super(Opcodes.ASM8);
        this.typeReceiver = typeReceiver;
        this.parent = owner;
        this.type = type;
    }

    @Override
    public void visitClassType(final String name) {
        this.typeReceiver.apply(this.parent, AccessPoint.create(ElementName.fromString(name)), type);

    }

}
