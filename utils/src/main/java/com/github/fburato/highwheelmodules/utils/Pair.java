package com.github.fburato.highwheelmodules.utils;

import java.util.Objects;

public final class Pair<A, B> {
    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A1, B1> Pair<A1, B1> make(A1 first, B1 second) {
        return new Pair<>(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {

        return Objects.hash(first, second);
    }
}
