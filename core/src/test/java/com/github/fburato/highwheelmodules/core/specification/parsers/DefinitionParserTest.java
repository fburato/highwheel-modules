package com.github.fburato.highwheelmodules.core.specification.parsers;

import com.github.fburato.highwheelmodules.core.specification.SyntaxTree;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DefinitionParserTest {

  private DefinitionParser testee = new DefinitionParser();

  @Test
  public void moduleDefinitionParserShouldParseIdentifierEqualStringLiteralNewLine() {
    assertParse(testee.moduleDefinitionParser, "module=\"regex\"\n", new SyntaxTree.ModuleDefinition("module", "regex"));
  }

  @Test(expected = RuntimeException.class)
  public void moduleDefinitionParserShouldFailOnAbsentNewLine() {
    assertParse(testee.moduleDefinitionParser, "module=\"regex\"", null);
  }

  @Test(expected = RuntimeException.class)
  public void moduleDefinitionParserShouldFailOnAbsentEqual() {
    assertParse(testee.moduleDefinitionParser, "module\"regex\"\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void moduleDefinitionParserShouldFailOnMalformedIdentifierEqual() {
    assertParse(testee.moduleDefinitionParser, "111module=\"regex\"\n", null);
  }

  @Test
  public void moduleDefinitionParserShouldParseMultipleRegex() {
    assertParse(testee.moduleDefinitionParser, "module=\"regex\",\"regex2\"\n",
        new SyntaxTree.ModuleDefinition("module", Arrays.asList("regex", "regex2")));
  }

  @Test(expected = RuntimeException.class)
  public void moduleDefinitionParserShouldFailOnCommaMissingRegex() {
    assertParse(testee.moduleDefinitionParser, "module=\"regex\",\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void moduleDefinitionParserShouldFailOnMissingRegexComma() {
    assertParse(testee.moduleDefinitionParser, "module=,\"regex\"\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void chainDependencyParserShouldFailOnOneIdentifier() {
    assertParse(testee.chainDependencyRuleParser, "id1\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void chainDependencyParserShouldFailOnOneIdentifierAndArrow() {
    assertParse(testee.chainDependencyRuleParser, "id1->\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void chainDependencyParserShouldFailOnNotArrow() {
    assertParse(testee.chainDependencyRuleParser, "id1-/->id2\n", null);
  }

  @Test
  public void chainDependencyRuleParserShouldParseWithJustRulesAndNoNewLine() {
    assertParse(testee.chainDependencyRuleParser, "id1->id2", new SyntaxTree.ChainDependencyRule(Arrays.asList("id1", "id2")));
  }

  @Test
  public void chainDependencyParserShouldWorkWithTwoIdentifiers() {
    assertParse(testee.chainDependencyRuleParser, "id1->id2\n",
        new SyntaxTree.ChainDependencyRule(Arrays.asList("id1", "id2")));
  }

  @Test
  public void chainDependencyParserShouldWorkWithManyIdentifiers() {
    assertParse(testee.chainDependencyRuleParser, "id1->id2->id3->id4\n",
        new SyntaxTree.ChainDependencyRule(Arrays.asList("id1", "id2", "id3", "id4")));
  }

  @Test
  public void noDependencyParserShouldParseWithJustRulesAndNoNewLine() {
    assertParse(testee.noDependecyRuleParser, "id1-/->id2", new SyntaxTree.NoDependentRule("id1", "id2"));
  }

  @Test(expected = RuntimeException.class)
  public void noDependencyParserShouldFailOnMoreIdAvailableNewLine() {
    assertParse(testee.noDependecyRuleParser, "id-/->id2-/->id3\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void noDependencyParserShouldFailOnMoreIdAvailableInterspersedWithDependency() {
    assertParse(testee.noDependecyRuleParser, "id-/->id2->id3\n", null);
  }

  @Test(expected = RuntimeException.class)
  public void noDependencyParserShouldFailIfOneIdentifierAbsent() {
    assertParse(testee.noDependecyRuleParser, "id-/->\n", null);
  }

  @Test
  public void noDependencyParserShouldReturnExpectedIdentifiers() {
    assertParse(testee.noDependecyRuleParser, "id1-/->id2\n", new SyntaxTree.NoDependentRule("id1", "id2"));
  }

  @Test
  public void rulesParserShouldReturnExpectedRules() {
    assertParse(testee.rulesParser, "id1->id2->id3\nid4-/->id5\nid6->id7\n", Arrays.asList(
        new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
        new SyntaxTree.NoDependentRule("id4", "id5"),
        new SyntaxTree.ChainDependencyRule("id6", "id7")
    ));
  }

  @Test
  public void rulesParserShouldIgnoreNewLinesExpectedRules() {
    assertParse(testee.rulesParser, "id1->id2->id3\n\n\nid6-/->id7\n", Arrays.asList(
        new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
        new SyntaxTree.NoDependentRule("id6", "id7")
    ));
  }

  @Test
  public void moduleDefinitionsParserShouldReturnExpectedRules() {
    assertParse(testee.moduleDefinitions, "module1=\"asdfasdf\"\nmodule2=\"ee323  2343 sdf\"\nmodule3=\"\"\n", Arrays.asList(
        new SyntaxTree.ModuleDefinition("module1", "asdfasdf"),
        new SyntaxTree.ModuleDefinition("module2", "ee323  2343 sdf"),
        new SyntaxTree.ModuleDefinition("module3", "")));
  }

  @Test
  public void moduleDefinitionsParserShouldIgnoreNewlinesAndReturnExpectedRules() {
    assertParse(testee.moduleDefinitions, "module1=\"asdfasdf\"\n\n\nmodule3=\"\"\n", Arrays.asList(
        new SyntaxTree.ModuleDefinition("module1", "asdfasdf"),
        new SyntaxTree.ModuleDefinition("module3", "")));
  }

  @Test
  public void prefixPreambleShouldParsePrefixColumnNewLineAndParseAdditionalNewLines() {
    assertParse(testee.prefixPreamble, "prefix:\n\n\n", null);
  }

  @Test
  public void modulesPreambleShouldParseModulesColumnNewLineAndParseAdditionalNewLines() {
    assertParse(testee.modulesPreamble, "modules:\n\n\n", null);
  }

  @Test
  public void rulesPreambleShouldParseRulesColumnNewLineAndParseAdditionalNewLines() {
    assertParse(testee.rulesPreamble, "rules:\n\n\n\n", null);
  }

  @Test
  public void prefixSectionShouldParsePreambleAndStringLiteralRegex() {
    assertParse(testee.prefixSection, "prefix:\n\n\n\n\"a regex\"\n\n\n\n", Optional.of("a regex"));
  }

  @Test
  public void modulesSectionShouldParsePreambleAndModulesDefinition() {
    assertParse(testee.modulesSection, "modules:\n\n\n\nm1=\"regex1.*\"\nm2=\"regex2+\"\n\n\n\n", Arrays.asList(
        new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
        new SyntaxTree.ModuleDefinition("m2", "regex2+")
    ));
  }

  @Test
  public void rulesSectionShouldParsePreambleAndRuleDefinitions() {
    assertParse(testee.rulesSection, "rules:\n\n\n\nid1->id2->id3\n\n\nid6-/->id7\n\nid8->id9\n", Arrays.asList(
        new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
        new SyntaxTree.NoDependentRule("id6", "id7"),
        new SyntaxTree.ChainDependencyRule("id8", "id9")
    ));
  }

  @Test
  public void rulesSectionShouldParsePreambleAndRuleWithEndOfFile() {
    assertParse(testee.rulesSection, "rules:\n\n\nida->idb", Arrays.asList(
        new SyntaxTree.ChainDependencyRule("ida", "idb")
    ));
  }

  @Test
  public void rulesSectionShouldParsePreambleAndNoDependecyRuleWithEndOfFile() {
    assertParse(testee.rulesSection, "rules:\n\n\nida->idb\nid2-/->id44", Arrays.asList(
        new SyntaxTree.ChainDependencyRule("ida", "idb"),
        new SyntaxTree.NoDependentRule("id2","id44")
    ));
  }

  @Test
  public void grammarShouldParseModuleAndRulesAndReturnTheDefinitionWithoutPrefix() {
    assertParse(testee.grammar, "modules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid1->id2->id3\nid6-/->id7\nid8->id9\n",
        new SyntaxTree.Definition(
            Arrays.asList(
                new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                new SyntaxTree.ModuleDefinition("m2", "regex2+")
            ), Arrays.asList(
            new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
            new SyntaxTree.NoDependentRule("id6", "id7"),
            new SyntaxTree.ChainDependencyRule("id8", "id9")
        )
        ));
  }

  @Test
  public void grammarShouldParsePrefixModuleAndRulesAndReturnTheDefinition() {
    assertParse(testee.grammar, "prefix:\"the prefix\"modules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid1->id2->id3\nid6-/->id7\nid8->id9\n",
        new SyntaxTree.Definition(
            Optional.of("the prefix"),
            Arrays.asList(
                new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                new SyntaxTree.ModuleDefinition("m2", "regex2+")
            ), Arrays.asList(
            new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
            new SyntaxTree.NoDependentRule("id6", "id7"),
            new SyntaxTree.ChainDependencyRule("id8", "id9")
        )
        ));
  }

  @Test
  public void parserShouldReadReadableIgnoringSpacesAndJavaCommentsAndReturnDefinitionWithoutPrefix() {
    final InputStreamReader reader =
        new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("./example-def.txt"));
    assertThat(testee.parse(reader)).isEqualTo(new SyntaxTree.Definition(
        Arrays.asList(
            new SyntaxTree.ModuleDefinition("Core",
                Arrays.asList("com.pitest.highwheel.core.*", "com.pitest.highwheel.core2.*")),
            new SyntaxTree.ModuleDefinition("Utils", "com.pitest.highwheel.utils.*"),
            new SyntaxTree.ModuleDefinition("Modules", "com.pitest.highwheel.modules.*"),
            new SyntaxTree.ModuleDefinition("Parser", "com.pitest.highwheel.parser.*")
        ), Arrays.asList(
        new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
        new SyntaxTree.NoDependentRule("Utils", "Core"),
        new SyntaxTree.NoDependentRule("Utils", "Parser"),
        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
        new SyntaxTree.ChainDependencyRule("Modules", "Utils")
    )));
  }

  @Test
  public void parserShouldReadReadableIgnoringSpacesAndJavaCommentsAndReturnDefinitionWithPrefix() {
    final InputStreamReader reader =
        new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("./example-def-with-prefix.txt"));
    assertThat(testee.parse(reader)).isEqualTo(new SyntaxTree.Definition(
        Optional.of("com.pitest.highwheel."),
        Arrays.asList(
            new SyntaxTree.ModuleDefinition("Core", Arrays.asList("core.*", "core2.*")),
            new SyntaxTree.ModuleDefinition("Utils", "utils.*"),
            new SyntaxTree.ModuleDefinition("Modules", "modules.*"),
            new SyntaxTree.ModuleDefinition("Parser", "parser.*")
        ), Arrays.asList(
        new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
        new SyntaxTree.NoDependentRule("Utils", "Core"),
        new SyntaxTree.NoDependentRule("Utils", "Parser"),
        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
        new SyntaxTree.ChainDependencyRule("Modules", "Utils")
    )));
  }

  private <T> void assertParse(Parser<T> parser, String source, T expected) {
    T actual = parser.from(testee.tp.tokeniser(), Parsers.EOF.skipMany()).parse(source);
    assertThat(actual).isEqualTo(expected);
  }
}
