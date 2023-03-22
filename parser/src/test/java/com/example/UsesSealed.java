package com.example;

public class UsesSealed {

    public void usesSealed() {
        final Sealed sealedClass = new SealedImplementation();
        sealedClass.foo();
    }
}
