package com.github.fburato.highwheelmodules.core.specification;

import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompilerTest {

    private final Compiler testee = new Compiler();

    private final HWModule CORE = HWModule.make("core", "core").get();
    private final HWModule COMMONS = HWModule.make("commons", "commons").get();
    private final HWModule MAIN = HWModule.make("main", "main").get();
    private final HWModule IO = HWModule.make("io", "io").get();

    @Test
    public void shouldFailIfRegularExpressionFailToParse() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Collections.singletonList(new SyntaxTree.ModuleDefinition("name", "invalidregex[")),
                    Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("name", "name")));
            testee.compile(definition);
        });
    }

    @Test
    public void shouldFailIfModuleNameAppearsTwice() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Arrays.asList(new SyntaxTree.ModuleDefinition("name", "name"),
                            new SyntaxTree.ModuleDefinition("name", "name")),
                    Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("name", "name")));
            testee.compile(definition);
        });
    }

    @Test
    public void shouldFailIfPrefixIsNotAValidRegex() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(Optional.of("invalidRegex["),
                    Collections.singletonList(new SyntaxTree.ModuleDefinition("name", "name")),
                    Collections.emptyList());
            testee.compile(definition);
        });
    }

    @Test
    public void shouldFailIfPrefixAvailableAndModuleRegexIsNotAValidRegex() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(Optional.of("prefix"),
                    Collections.singletonList(new SyntaxTree.ModuleDefinition("name", "invalidRegex[")),
                    Collections.emptyList());
            testee.compile(definition);
        });
    }

    @Test
    public void shouldPrefixAllModulesRegexWithPrefix() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(Optional.of("org.example."),
                Arrays.asList(new SyntaxTree.ModuleDefinition("Foo", Arrays.asList("foo.*", "foobar.*")),
                        new SyntaxTree.ModuleDefinition("Bar", "bar.*")),
                Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("Foo", "Bar")));

        final Definition actual = testee.compile(definition);
        assertThat(actual.modules).containsExactlyInAnyOrder(
                HWModule.make("Foo", "org.example.foo.*", "org.example.foobar.*").get(),
                HWModule.make("Bar", "org.example.bar.*").get());
    }

    @Test
    public void shouldFailIfRuleReferToNotDefinedModules() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Collections.singletonList(new SyntaxTree.ModuleDefinition("name", "regex")),
                    Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("name", "name1")));
            testee.compile(definition);
        });
    }

    @Test
    public void shouldReturnTheExpectedModules() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                        new SyntaxTree.ModuleDefinition("commons", "commons"),
                        new SyntaxTree.ModuleDefinition("main", "main"), new SyntaxTree.ModuleDefinition("io", "io")),
                Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("core", "commons")));
        Definition actual = testee.compile(definition);
        assertThat(actual.modules).containsExactlyInAnyOrder(CORE, COMMONS, IO, MAIN);
    }

    @Test
    public void shouldConvertChainDependencyInTwoByTwoDependencies() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                        new SyntaxTree.ModuleDefinition("commons", "commons"),
                        new SyntaxTree.ModuleDefinition("main", "main"), new SyntaxTree.ModuleDefinition("io", "io")),
                Collections.<SyntaxTree.Rule> singletonList(
                        new SyntaxTree.ChainDependencyRule("main", "core", "commons")));
        Definition actual = testee.compile(definition);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(new Dependency(MAIN, CORE),
                new Dependency(CORE, COMMONS));
    }

    @Test
    public void shouldConvertNoDependencyRulesAppropriately() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                        new SyntaxTree.ModuleDefinition("commons", "commons"),
                        new SyntaxTree.ModuleDefinition("main", "main"), new SyntaxTree.ModuleDefinition("io", "io")),
                Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.NoDependentRule("core", "io")));
        Definition actual = testee.compile(definition);
        assertThat(actual.noStrictDependencies).containsExactlyInAnyOrder(new NoStrictDependency(CORE, IO));
    }
}
