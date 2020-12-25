package com.example;

public interface AnInterface {
    default String bar() {
        return "bar";
    }
}
