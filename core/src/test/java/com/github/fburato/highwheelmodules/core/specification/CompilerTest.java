package com.github.fburato.highwheelmodules.core.specification;

import com.github.fburato.highwheelmodules.core.testutils.SyntaxTreeDefinitionBuilder;
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode;
import com.github.fburato.highwheelmodules.model.modules.AnonymousModule;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("should convert one to many dependency in two by two dependencies")
    void testOneToManyCompile() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                        new SyntaxTree.ModuleDefinition("commons", "commons"),
                        new SyntaxTree.ModuleDefinition("main", "main"), new SyntaxTree.ModuleDefinition("io", "io")),
                Collections.<SyntaxTree.Rule> singletonList(
                        new SyntaxTree.OneToManyRule("main", Arrays.asList("core", "commons"))));
        Definition actual = testee.compile(definition);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(new Dependency(MAIN, CORE),
                new Dependency(MAIN, COMMONS));
    }

    @Test
    @DisplayName("should fail to compile one to many if starting module does not exist")
    void testOneToManyFailOnStarting() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Arrays.asList(new SyntaxTree.ModuleDefinition("a", "regex1"),
                            new SyntaxTree.ModuleDefinition("b", "regex2")),
                    Collections.<SyntaxTree.Rule> singletonList(
                            new SyntaxTree.OneToManyRule("c", Arrays.asList("a", "b"))));
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail to compile one to many if any ending module does not exist")
    void testOneToManyFailOnEnding() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Arrays.asList(new SyntaxTree.ModuleDefinition("a", "regex1"),
                            new SyntaxTree.ModuleDefinition("b", "regex2")),
                    Collections.<SyntaxTree.Rule> singletonList(
                            new SyntaxTree.OneToManyRule("a", Arrays.asList("b", "c"))));
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should convert many to one dependency in two by two dependencies")
    void testManyToManyCompile() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                        new SyntaxTree.ModuleDefinition("commons", "commons"),
                        new SyntaxTree.ModuleDefinition("main", "main"), new SyntaxTree.ModuleDefinition("io", "io")),
                Collections.<SyntaxTree.Rule> singletonList(
                        new SyntaxTree.ManyToOneRule(Arrays.asList("core", "commons"), "io")));
        Definition actual = testee.compile(definition);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(new Dependency(CORE, IO),
                new Dependency(COMMONS, IO));
    }

    @Test
    @DisplayName("should fail to compile many to one if any starting module does not exist")
    void testManyToOneFailOnStarting() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Arrays.asList(new SyntaxTree.ModuleDefinition("a", "regex1"),
                            new SyntaxTree.ModuleDefinition("b", "regex2")),
                    Collections.<SyntaxTree.Rule> singletonList(
                            new SyntaxTree.ManyToOneRule(Arrays.asList("b", "c"), "a")));
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail to compile many to one if any ending module does not exist")
    void testManyToOneFailOnEnding() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(
                    Arrays.asList(new SyntaxTree.ModuleDefinition("a", "regex1"),
                            new SyntaxTree.ModuleDefinition("b", "regex2")),
                    Collections.<SyntaxTree.Rule> singletonList(
                            new SyntaxTree.ManyToOneRule(Arrays.asList("a", "b"), "c")));
            testee.compile(definition);
        });
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

    @Test
    @DisplayName("should fail if whitelist contains malformed regex")
    void testFailWhiteList() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(Optional.empty(),
                    Optional.of(Arrays.asList("", "[[123")), Optional.empty(),
                    Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                            new SyntaxTree.ModuleDefinition("commons", "commons"),
                            new SyntaxTree.ModuleDefinition("main", "main"),
                            new SyntaxTree.ModuleDefinition("io", "io")),
                    Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.NoDependentRule("core", "io")));
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail if whitelist contains malformed regex")
    void testFailBlacList() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = new SyntaxTree.Definition(Optional.empty(), Optional.empty(),
                    Optional.of(Arrays.asList("dd", "", "[[1asdf   asdf")),
                    Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                            new SyntaxTree.ModuleDefinition("commons", "commons"),
                            new SyntaxTree.ModuleDefinition("main", "main"),
                            new SyntaxTree.ModuleDefinition("io", "io")),
                    Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.NoDependentRule("core", "io")));
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should compile definition with whitelist and blacklist")
    void testCompileWhiteBlack() {
        final SyntaxTree.Definition definition = new SyntaxTree.Definition(Optional.of("org.example."),
                Optional.of(Arrays.asList("a", "b")), Optional.of(Arrays.asList("c", "d")),
                Arrays.asList(new SyntaxTree.ModuleDefinition("Foo", Arrays.asList("foo.*", "foobar.*")),
                        new SyntaxTree.ModuleDefinition("Bar", "bar.*")),
                Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("Foo", "Bar")));
        Definition actual = testee.compile(definition);
        final HWModule Foo = HWModule.make("Foo", "org.example.foo.*", "org.example.foobar.*").get();
        final HWModule Bar = HWModule.make("Bar", "org.example.bar.*").get();
        assertThat(actual.modules).containsExactlyInAnyOrder(Foo, Bar);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(new Dependency(Foo, Bar));
        assertThat(actual.whitelist).contains(AnonymousModule.make(Arrays.asList("a", "b")).get());
        assertThat(actual.blackList).contains(AnonymousModule.make(Arrays.asList("c", "d")).get());
    }

    private SyntaxTreeDefinitionBuilder minimumDefBuilder = SyntaxTreeDefinitionBuilder.baseBuilder().with($ -> {
        $.moduleDefinitions = Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                new SyntaxTree.ModuleDefinition("commons", "commons"));
        $.rules = Collections.<SyntaxTree.Rule> singletonList(new SyntaxTree.ChainDependencyRule("core", "commons"));
        $.mode = Optional.of("STRICT");
    });

    @Test
    @DisplayName("should set mode to strict if specification does not contain mode explicitly")
    void testDefaultMode() {
        final SyntaxTree.Definition definition = minimumDefBuilder.build();

        final Definition actual = testee.compile(definition);
        assertThat(actual.mode).isEqualTo(AnalysisMode.STRICT);
    }

    @Test
    @DisplayName("should set mode to STRICT if specification uses STRICT explicit as mode")
    void testExplicitStrict() {
        final SyntaxTree.Definition definition = minimumDefBuilder.with($ -> $.mode = Optional.of("STRICT")).build();
        final Definition actual = testee.compile(definition);
        assertThat(actual.mode).isEqualTo(AnalysisMode.STRICT);
    }

    @Test
    @DisplayName("should set mode to loose if specification uses LOOSE explicitly as mode")
    void testExplicitLoose() {
        final SyntaxTree.Definition definition = minimumDefBuilder.with($ -> $.mode = Optional.of("LOOSE")).build();
        final Definition actual = testee.compile(definition);
        assertThat(actual.mode).isEqualTo(AnalysisMode.LOOSE);
    }

    @Test
    @DisplayName("should fail to compile if mode is not recognised")
    void testFailMode() {
        final SyntaxTree.Definition definition = minimumDefBuilder.with($ -> $.mode = Optional.of("NOT_A_MODE"))
                .build();
        assertThrows(CompilerException.class, () -> testee.compile(definition));
    }
}
