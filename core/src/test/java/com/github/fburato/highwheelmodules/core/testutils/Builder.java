package com.github.fburato.highwheelmodules.core.testutils;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class Builder<T, G extends Builder<T, G>> {

    protected final ArrayList<Consumer<G>> buildSequence;

    protected Builder(ArrayList<Consumer<G>> buildSequence) {
        this.buildSequence = buildSequence;
    }

    protected abstract G copy();

    protected abstract T makeValue();

    public G with(Consumer<G> consumer) {
        final G nextBuilder = this.copy();
        nextBuilder.buildSequence.add(consumer);
        return nextBuilder;
    }

    public T build() {
        final G instance = this.copy();
        instance.buildSequence.forEach(c -> c.accept(instance));
        return instance.makeValue();
    }
}
