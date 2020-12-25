package com.example;

public class UsesAnInterfaceInMethod {

    public void foo() {
        AnInterface anInterface = new ImplementsAnInterface();
        System.out.println(anInterface.bar());
    }
}
