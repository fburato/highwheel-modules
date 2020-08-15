package com.github.fburato.highwheelmodules.core.specification;

import com.github.fburato.highwheelmodules.core.specification.SyntaxTree.ChainDependencyRule;
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

@DisplayName("Compiler compilation")
class CompilerTest {

    private final Compiler testee = new Compiler();

    private final HWModule CORE = HWModule.make("core", "core").get();
    private final HWModule COMMONS = HWModule.make("commons", "commons").get();
    private final HWModule MAIN = HWModule.make("main", "main").get();
    private final HWModule IO = HWModule.make("io", "io").get();
    private final SyntaxTree.Definition.DefinitionBuilder definitionBuilder = SyntaxTree.Definition.DefinitionBuilder
            .baseBuilder();

    @Test
    @DisplayName("should fail of module regular expression fails to parse")
    void testFailModuleRegex() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
                $.moduleDefinitions = Collections
                        .singletonList(new SyntaxTree.ModuleDefinition("name", "invalidregex["));
                $.rules = Collections.singletonList(new ChainDependencyRule("name", "name"));
            }).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail if module name appears twice")
    void testFailRepeatedModule() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
                $.moduleDefinitions = Arrays.asList(new SyntaxTree.ModuleDefinition("name", "name"),
                        new SyntaxTree.ModuleDefinition("name", "name"));
                $.rules = Collections.singletonList(new ChainDependencyRule("name", "name"));
            }).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail if prefix is not a valid regex")
    void testFailPrefixRegex() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
                $.prefix = Optional.of("invalidRegex[");
                $.moduleDefinitions = Collections.singletonList(new SyntaxTree.ModuleDefinition("name", "name"));
                $.rules = Collections.emptyList();
            }).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail if prefix is valid and module regex is invalid")
    void testFailPrefixAndModuleRegex() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
                $.prefix = Optional.of("prefix");
                $.moduleDefinitions = Collections
                        .singletonList(new SyntaxTree.ModuleDefinition("name", "invalidRegex["));
                $.rules = Collections.emptyList();
            }).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should add prefix to all modules regex")
    void testPrependPrefix() {
        final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
            $.prefix = Optional.of("org.example.");
            $.moduleDefinitions = Arrays.asList(
                    new SyntaxTree.ModuleDefinition("Foo", Arrays.asList("foo.*", "foobar.*")),
                    new SyntaxTree.ModuleDefinition("Bar", "bar.*"));
            $.rules = Collections.singletonList(new ChainDependencyRule("Foo", "Bar"));
        }).build();

        final Definition actual = testee.compile(definition);
        assertThat(actual.modules).containsExactlyInAnyOrder(
                HWModule.make("Foo", "org.example.foo.*", "org.example.foobar.*").get(),
                HWModule.make("Bar", "org.example.bar.*").get());
    }

    @Test
    @DisplayName("should fail if rules refer to undefined modules")
    void testFailNotAvailableModule() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
                $.moduleDefinitions = Collections.singletonList(new SyntaxTree.ModuleDefinition("name", "regex"));
                $.rules = Collections.singletonList(new ChainDependencyRule("name", "name1"));
            }).build();
            testee.compile(definition);
        });
    }

    private SyntaxTree.Definition.DefinitionBuilder definitionBuilderWithModules = definitionBuilder
            .with($ -> $.moduleDefinitions = Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                    new SyntaxTree.ModuleDefinition("commons", "commons"),
                    new SyntaxTree.ModuleDefinition("main", "main"), new SyntaxTree.ModuleDefinition("io", "io")));

    @Test
    @DisplayName("should return the expected modules")
    void testExpectedModules() {
        final SyntaxTree.Definition definition = definitionBuilderWithModules.build();
        Definition actual = testee.compile(definition);
        assertThat(actual.modules).containsExactlyInAnyOrder(CORE, COMMONS, IO, MAIN);
    }

    @Test
    @DisplayName("should convert chain dependencies in two-by-two dependencies")
    void testChainDependencyConversion() {
        final SyntaxTree.Definition definition = definitionBuilderWithModules
                .with($ -> $.rules = Collections.singletonList(new ChainDependencyRule("main", "core", "commons")))
                .build();
        Definition actual = testee.compile(definition);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(Dependency.make(MAIN, CORE),
                Dependency.make(CORE, COMMONS));
    }

    @Test
    @DisplayName("should convert one to many dependency in two by two dependencies")
    void testOneToManyCompile() {
        final SyntaxTree.Definition definition = definitionBuilderWithModules.with($ -> $.rules = Collections
                .singletonList(new SyntaxTree.OneToManyRule("main", Arrays.asList("core", "commons")))).build();
        Definition actual = testee.compile(definition);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(Dependency.make(MAIN, CORE),
                Dependency.make(MAIN, COMMONS));
    }

    @Test
    @DisplayName("should fail to compile one to many if starting module does not exist")
    void testOneToManyFailOnStarting() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilderWithModules.with($ -> $.rules = Collections
                    .singletonList(new SyntaxTree.OneToManyRule("c", Arrays.asList("io", "core")))).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail to compile one to many if any ending module does not exist")
    void testOneToManyFailOnEnding() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilderWithModules.with($ -> $.rules = Collections
                    .singletonList(new SyntaxTree.OneToManyRule("io", Arrays.asList("core", "c")))).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should convert many to one dependency in two by two dependencies")
    void testManyToManyCompile() {
        final SyntaxTree.Definition definition = definitionBuilderWithModules.with($ -> $.rules = Collections
                .singletonList(new SyntaxTree.ManyToOneRule(Arrays.asList("core", "commons"), "io"))).build();
        Definition actual = testee.compile(definition);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(Dependency.make(CORE, IO),
                Dependency.make(COMMONS, IO));
    }

    @Test
    @DisplayName("should fail to compile many to one if any starting module does not exist")
    void testManyToOneFailOnStarting() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilderWithModules.with($ -> $.rules = Collections
                    .singletonList(new SyntaxTree.ManyToOneRule(Arrays.asList("core", "c"), "io"))).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail to compile many to one if any ending module does not exist")
    void testManyToOneFailOnEnding() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilderWithModules.with($ -> $.rules = Collections
                    .singletonList(new SyntaxTree.ManyToOneRule(Arrays.asList("core", "io"), "c"))).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should convert NoDependentRule appropriately")
    void testNoDependentRuleConversion() {
        final SyntaxTree.Definition definition = definitionBuilderWithModules
                .with($ -> $.rules = Collections.singletonList(new SyntaxTree.NoDependentRule("core", "io"))).build();
        Definition actual = testee.compile(definition);
        assertThat(actual.noStrictDependencies).containsExactlyInAnyOrder(NoStrictDependency.make(CORE, IO));
    }

    @Test
    @DisplayName("should fail if whitelist contains malformed regex")
    void testFailWhiteList() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilderWithModules
                    .with($ -> $.whiteList = Optional.of(Arrays.asList("", "[[123"))

                    ).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should fail if blacklist contains malformed regex")
    void testFailBlacList() {
        assertThrows(CompilerException.class, () -> {
            final SyntaxTree.Definition definition = definitionBuilderWithModules
                    .with($ -> $.blackList = Optional.of(Arrays.asList("dd", "", "[[1asdf   asdf"))

                    ).build();
            testee.compile(definition);
        });
    }

    @Test
    @DisplayName("should compile definition with whitelist, blacklist and prefix")
    void testCompileWhiteBlack() {
        final SyntaxTree.Definition definition = definitionBuilder.with($ -> {
            $.prefix = Optional.of("org.example.");
            $.whiteList = Optional.of(Arrays.asList("a", "b"));
            $.blackList = Optional.of(Arrays.asList("c", "d"));
            $.moduleDefinitions = Arrays.asList(
                    new SyntaxTree.ModuleDefinition("Foo", Arrays.asList("foo.*", "foobar.*")),
                    new SyntaxTree.ModuleDefinition("Bar", "bar.*"));
            $.rules = Collections.singletonList(new ChainDependencyRule("Foo", "Bar"));
        }

        ).build();
        Definition actual = testee.compile(definition);
        final HWModule Foo = HWModule.make("Foo", "org.example.foo.*", "org.example.foobar.*").get();
        final HWModule Bar = HWModule.make("Bar", "org.example.bar.*").get();
        assertThat(actual.modules).containsExactlyInAnyOrder(Foo, Bar);
        assertThat(actual.dependencies).containsExactlyInAnyOrder(Dependency.make(Foo, Bar));
        assertThat(actual.whitelist).contains(AnonymousModule.make(Arrays.asList("a", "b")).get());
        assertThat(actual.blackList).contains(AnonymousModule.make(Arrays.asList("c", "d")).get());
    }

    private SyntaxTree.Definition.DefinitionBuilder minimumDefBuilder = SyntaxTree.Definition.DefinitionBuilder
            .baseBuilder().with($ -> {
                $.moduleDefinitions = Arrays.asList(new SyntaxTree.ModuleDefinition("core", "core"),
                        new SyntaxTree.ModuleDefinition("commons", "commons"));
                $.rules = Collections.singletonList(new ChainDependencyRule("core", "commons"));
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
