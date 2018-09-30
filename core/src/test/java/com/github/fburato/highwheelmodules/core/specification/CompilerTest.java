package com.github.fburato.highwheelmodules.core.specification;

import com.github.fburato.highwheelmodules.core.model.Definition;
import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.rules.Dependency;
import com.github.fburato.highwheelmodules.core.model.rules.NoStrictDependency;
import org.junit.Test;

import java.util.Arrays;



import static org.assertj.core.api.Assertions.*;

public class CompilerTest {

  private final Compiler testee = new Compiler();

  private final Module CORE = Module.make("core", "core").get();
  private final Module COMMONS = Module.make("commons", "commons").get();
  private final Module MAIN = Module.make("main", "main").get();
  private final Module IO = Module.make("io", "io").get();

  @Test(expected = CompilerException.class)
  public void shouldFailIfRegularExpressionFailToParse() {
    final SyntaxTree.Definition definition = new SyntaxTree.Definition(
        Arrays.asList(new SyntaxTree.ModuleDefinition("name", "invalidregex[")),
        Arrays.<SyntaxTree.Rule>asList(new SyntaxTree.ChainDependencyRule("name", "name")));
    testee.compile(definition);
  }

  @Test(expected = CompilerException.class)
  public void shouldFailIfModuleNameAppearsTwice() {
    final SyntaxTree.Definition definition = new SyntaxTree.Definition(
        Arrays.asList(
            new SyntaxTree.ModuleDefinition("name", "name"),
            new SyntaxTree.ModuleDefinition("name", "name")
        ),
        Arrays.<SyntaxTree.Rule>asList(new SyntaxTree.ChainDependencyRule("name", "name")));
    testee.compile(definition);
  }

  @Test(expected = CompilerException.class)
  public void shouldFailIfRuleReferToNotDefinedModules() {
    final SyntaxTree.Definition definition = new SyntaxTree.Definition(
        Arrays.asList(new SyntaxTree.ModuleDefinition("name", "regex")),
        Arrays.<SyntaxTree.Rule>asList(new SyntaxTree.ChainDependencyRule("name", "name1")));
    testee.compile(definition);
  }

  @Test
  public void shouldReturnTheExpectedModules() {
    final SyntaxTree.Definition definition = new SyntaxTree.Definition(
        Arrays.asList(
            new SyntaxTree.ModuleDefinition("core", "core"),
            new SyntaxTree.ModuleDefinition("commons", "commons"),
            new SyntaxTree.ModuleDefinition("main", "main"),
            new SyntaxTree.ModuleDefinition("io", "io")
        ),
        Arrays.<SyntaxTree.Rule>asList(new SyntaxTree.ChainDependencyRule("core", "commons")));
    Definition actual = testee.compile(definition);
    assertThat(actual.modules).containsAll(Arrays.asList(CORE, COMMONS, IO, MAIN));
  }

  @Test
  public void shouldConvertChainDependencyInTwoByTwoDependencies() {
    final SyntaxTree.Definition definition = new SyntaxTree.Definition(
        Arrays.asList(
            new SyntaxTree.ModuleDefinition("core", "core"),
            new SyntaxTree.ModuleDefinition("commons", "commons"),
            new SyntaxTree.ModuleDefinition("main", "main"),
            new SyntaxTree.ModuleDefinition("io", "io")
        ),
        Arrays.<SyntaxTree.Rule>asList(new SyntaxTree.ChainDependencyRule("main", "core", "commons")));
    Definition actual = testee.compile(definition);
    assertThat(actual.dependencies)
        .containsAll(Arrays.asList(new Dependency(MAIN, CORE), new Dependency(CORE, COMMONS)));
  }

  @Test
  public void shouldConvertNoDependencyRulesAppropriately() {
    final SyntaxTree.Definition definition = new SyntaxTree.Definition(
        Arrays.asList(
            new SyntaxTree.ModuleDefinition("core", "core"),
            new SyntaxTree.ModuleDefinition("commons", "commons"),
            new SyntaxTree.ModuleDefinition("main", "main"),
            new SyntaxTree.ModuleDefinition("io", "io")
        ),
        Arrays.<SyntaxTree.Rule>asList(new SyntaxTree.NoDependentRule("core", "io")));
    Definition actual = testee.compile(definition);
    assertThat(actual.noStrictDependencies).containsAll(Arrays.asList(new NoStrictDependency(CORE, IO)));
  }
}
