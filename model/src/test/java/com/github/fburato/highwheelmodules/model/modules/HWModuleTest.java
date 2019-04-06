package com.github.fburato.highwheelmodules.model.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HWModule")
public class HWModuleTest {
    private final String NAME = "NAME";

    @Test
    @DisplayName("equals should work on the pattern literals")
    void testEqualsGlobs() {
        assertThat(HWModule.make(NAME, "a", "b")).isEqualTo(HWModule.make(NAME, "a", "b"));
        assertThat(HWModule.make(NAME, "a")).isNotEqualTo(HWModule.make(NAME, "b"));
    }

    @Test
    @DisplayName("equals should work on the module name")
    void testEqualsName() {
        assertThat(HWModule.make(NAME, "a", "b")).isNotEqualTo(HWModule.make("otherName", "a", "b"));
    }

    @Test
    @DisplayName("equals should work on the pattern literals in different order")
    void testEqualsGlobOrder() {
        assertThat(HWModule.make(NAME, "a", "b")).isEqualTo(HWModule.make(NAME, "b", "a"));
    }

    @TestFactory
    @DisplayName("contains")
    public List<DynamicTest> containsAnonymous() {
        return AnonymousModuleTest.containsTests(globs -> HWModule.make(NAME, globs));
    }

    @TestFactory
    @DisplayName("make")
    public List<DynamicTest> makeAnonymous() {
        return AnonymousModuleTest.makeTests(globs -> HWModule.make(NAME, globs));
    }
}
