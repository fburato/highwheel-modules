package com.example;

public record TestRecord(Foo foo) {

    void aFoo(Foo aFoo) {

    }
    void bar() {
        final Foo foo = new Foo();
        System.out.println(foo);
    }

    Foo baz() {
        return null;
    }
}
