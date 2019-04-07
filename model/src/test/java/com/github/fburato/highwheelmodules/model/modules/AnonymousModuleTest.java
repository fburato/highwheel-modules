package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@DisplayName("AnonymousModule")
public class AnonymousModuleTest {
    @FunctionalInterface
    public interface ModuleBuilder<T extends MatchingModule> {
        Optional<T> make(String... globs);
    }

    public static <T extends MatchingModule> List<DynamicTest> containsTests(ModuleBuilder<T> builder) {
        final String GLOB = "org.pitest.foo*";
        final T testee = builder.make(GLOB).get();
        return Arrays.asList(
                dynamicTest("should return true if element name matches pattern",
                        () -> assertThat(testee.contains(new ElementName("org.pitest.foo.Something"))).isTrue()),
                dynamicTest("should return true when multiple matchers are available", () -> {
                    final MatchingModule otherTestee = builder.make("a*", "b*").get();
                    assertThat(otherTestee.contains(new ElementName("afoo"))).isTrue();
                    assertThat(otherTestee.contains(new ElementName("bfoo"))).isTrue();
                }),
                dynamicTest("should return false if element name does not match pattern",
                        () -> assertThat(testee.contains(new ElementName("not.pitest.foo"))).isFalse()),
                dynamicTest("should return false if the element name does not match any pattern", () -> {
                    final MatchingModule otherTestee = builder.make("a*", "b*").get();
                    assertThat(otherTestee.contains(new ElementName("cfoo"))).isFalse();
                }));
    }

    public static <T extends MatchingModule> List<DynamicTest> makeTests(ModuleBuilder<T> builder) {
        return Arrays.asList(
                dynamicTest("should be empty if regex passed is invalid",
                        () -> assertThat(builder.make("[asdf")).isEmpty()),
                dynamicTest("should be empty if any regex passed is invalid",
                        () -> assertThat(builder.make("valid", "[asdf")).isEmpty()),
                dynamicTest("should be defined if regex passed is valid",
                        () -> assertThat(builder.make("org.pitest.*")).isNotEmpty()),
                dynamicTest("should be defined if all regexes passed are valid",
                        () -> assertThat(builder.make("org.pitest.*", "valid")).isNotEmpty()));
    }

    @Test
    @DisplayName("equals should work on the pattern literals")
    void testEquals() {
        assertThat(AnonymousModule.make("a", "b")).isEqualTo(AnonymousModule.make("a", "b"));
        assertThat(AnonymousModule.make("a")).isNotEqualTo(AnonymousModule.make("b"));
    }

    @Test
    @DisplayName("equals should work on the pattern literals in different order")
    void testEqualsOrder() {
        assertThat(AnonymousModule.make("a", "b")).isEqualTo(AnonymousModule.make("b", "a"));
    }

    @TestFactory
    @DisplayName("contains")
    public List<DynamicTest> containsAnonymous() {
        return containsTests(AnonymousModule::make);
    }

    @TestFactory
    @DisplayName("make")
    public List<DynamicTest> makeAnonymous() {
        return makeTests(AnonymousModule::make);
    }
}
