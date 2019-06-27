package com.github.fburato.highwheelmodules.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Builder")
public class BuilderTest {

    private static class A {
        final String a;
        int b;

        A(String a) {
            this.a = a;
        }

        void setB(int b) {
            this.b = b;
        }
    }

    private static class ABuilder extends Builder<A, ABuilder> {

        String a;
        int b;

        private ABuilder() {
            super(ABuilder::new);
        }

        @Override
        protected A makeValue() {
            final A value = new A(a);
            value.setB(b);
            return value;
        }

        static ABuilder baseBuilder() {
            return new ABuilder();
        }
    }

    @Test
    @DisplayName("should chain with clauses")
    void testChain() {
        final String expectedA = "hello";
        final int expectedB = 42;

        A actual = ABuilder.baseBuilder().with($ -> $.a = expectedA).with($ -> $.b = expectedB).build();

        assertThat(actual.a).isEqualTo(expectedA);
        assertThat(actual.b).isEqualTo(expectedB);
    }

    @Test
    @DisplayName("should apply 'with' changes in order")
    void testOrder() {
        final String firstA = "foo";
        final String secondA = "bar";

        A actual = ABuilder.baseBuilder().with($ -> $.a = firstA).with($ -> $.a = secondA).build();
        assertThat(actual.a).isEqualTo(secondA);
    }

    @Test
    @DisplayName("should build different objects with the same values")
    void testSame() {
        final String expectedA = "hello 1";
        final int expectedB = -42;
        ABuilder builder = ABuilder.baseBuilder().with($ -> {
            $.a = expectedA;
            $.b = expectedB;
        });

        A actual1 = builder.build(), actual2 = builder.build();

        assertThat(actual1).isNotSameAs(actual2);
        assertThat(actual1.a).isEqualTo(expectedA);
        assertThat(actual1.b).isEqualTo(expectedB);
        assertThat(actual2.a).isEqualTo(expectedA);
        assertThat(actual2.b).isEqualTo(expectedB);
    }

    @Test
    @DisplayName("should remain immutable after chaining")
    void testImmutable() {
        final String expectedA = "hello 2";
        final int expectedB = 1045;

        Consumer<ABuilder> aSetter = $ -> $.a = expectedA;
        Consumer<ABuilder> bSetter = $ -> $.b = expectedB;

        ABuilder builder1 = ABuilder.baseBuilder().with(aSetter);
        ABuilder builder2 = builder1.with(bSetter);

        assertThat(builder1.buildSequence).containsExactly(aSetter);
        assertThat(builder2.buildSequence).containsExactly(aSetter, bSetter);
    }
}
