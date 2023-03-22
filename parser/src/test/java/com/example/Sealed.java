package com.example;

public sealed interface Sealed {
    default void foo(){}
}

final class SealedImplementation implements Sealed {
}
