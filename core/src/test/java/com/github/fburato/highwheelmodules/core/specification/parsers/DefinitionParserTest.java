package com.github.fburato.highwheelmodules.core.specification.parsers;

import com.github.fburato.highwheelmodules.core.specification.SyntaxTree;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefinitionParserTest {

    private DefinitionParser testee = new DefinitionParser();

    @Test
    public void moduleDefinitionParserShouldParseIdentifierEqualStringLiteralNewLine() {
        assertParse(testee.moduleDefinitionParser, "module=\"regex\"\n",
                new SyntaxTree.ModuleDefinition("module", "regex"));
    }

    @Test
    public void moduleDefinitionParserShouldFailOnAbsentNewLine() {
        assertThrows(RuntimeException.class,
                () -> assertParse(testee.moduleDefinitionParser, "module=\"regex\"", null));
    }

    @Test
    public void moduleDefinitionParserShouldFailOnAbsentEqual() {
        assertThrows(RuntimeException.class,
                () -> assertParse(testee.moduleDefinitionParser, "module\"regex\"\n", null));
    }

    @Test
    public void moduleDefinitionParserShouldFailOnMalformedIdentifierEqual() {
        assertThrows(RuntimeException.class,
                () -> assertParse(testee.moduleDefinitionParser, "111module=\"regex\"\n", null));
    }

    @Test
    public void moduleDefinitionParserShouldParseMultipleRegex() {
        assertParse(testee.moduleDefinitionParser, "module=\"regex\",\"regex2\"\n",
                new SyntaxTree.ModuleDefinition("module", Arrays.asList("regex", "regex2")));
    }

    @Test
    public void moduleDefinitionParserShouldFailOnCommaMissingRegex() {
        assertThrows(RuntimeException.class,
                () -> assertParse(testee.moduleDefinitionParser, "module=\"regex\",\n", null));
    }

    @Test
    public void moduleDefinitionParserShouldFailOnMissingRegexComma() {
        assertThrows(RuntimeException.class,
                () -> assertParse(testee.moduleDefinitionParser, "module=,\"regex\"\n", null));
    }

    @Test
    public void chainDependencyParserShouldFailOnOneIdentifier() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.chainDependencyRuleParser, "id1\n", null));
    }

    @Test
    public void chainDependencyParserShouldFailOnOneIdentifierAndArrow() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.chainDependencyRuleParser, "id1->\n", null));
    }

    @Test
    public void chainDependencyParserShouldFailOnNotArrow() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.chainDependencyRuleParser, "id1-/->id2\n", null));
    }

    @Test
    public void chainDependencyRuleParserShouldParseWithJustRulesAndNoNewLine() {
        assertParse(testee.chainDependencyRuleParser, "id1->id2",
                new SyntaxTree.ChainDependencyRule(Arrays.asList("id1", "id2")));
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

    @Test
    public void noDependencyParserShouldFailOnMoreIdAvailableNewLine() {
        assertThrows(RuntimeException.class,
                () -> assertParse(testee.noDependecyRuleParser, "id-/->id2-/->id3\n", null));
    }

    @Test
    public void noDependencyParserShouldFailOnMoreIdAvailableInterspersedWithDependency() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.noDependecyRuleParser, "id-/->id2->id3\n", null));
    }

    @Test
    public void noDependencyParserShouldFailIfOneIdentifierAbsent() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.noDependecyRuleParser, "id-/->\n", null));
    }

    @Test
    public void noDependencyParserShouldReturnExpectedIdentifiers() {
        assertParse(testee.noDependecyRuleParser, "id1-/->id2\n", new SyntaxTree.NoDependentRule("id1", "id2"));
    }

    @Nested
    @DisplayName("oneToManyRuleParser should")
    class OneToManyRuleParserTests {
        @Test
        @DisplayName("fail on empty many")
        void testFailEmptyMany() {
            assertThrows(RuntimeException.class, () -> assertParse(testee.oneToManyRuleParser, "id1->()\n", null));
        }

        @Test
        @DisplayName("fail on chained arrow operators")
        void testFailChain() {
            assertThrows(RuntimeException.class,
                    () -> assertParse(testee.oneToManyRuleParser, "id1->(id2,id3)->id4", null));
            assertThrows(RuntimeException.class,
                    () -> assertParse(testee.oneToManyRuleParser, "id0->id1->(id2,id3)", null));
        }

        @Test
        @DisplayName("parse definition with newline")
        void testNewLine() {
            assertParse(testee.oneToManyRuleParser, "id0->(id1,id2)\n",
                    new SyntaxTree.OneToManyRule("id0", Arrays.asList("id1", "id2")));
        }

        @Test
        @DisplayName("parse definition without newline")
        void testNoNewLine() {
            assertParse(testee.oneToManyRuleParser, "id1->(id2,id3)",
                    new SyntaxTree.OneToManyRule("id1", Arrays.asList("id2", "id3")));
        }

        @Test
        @DisplayName("parse definition with singleton")
        void testSingleton() {
            assertParse(testee.oneToManyRuleParser, "id2->(id3)",
                    new SyntaxTree.OneToManyRule("id2", Collections.singletonList("id3")));
        }
    }

    @Nested
    @DisplayName("manyToOneRuleParser should")
    class ManyToOneRuleParserTests {
        @Test
        @DisplayName("fail on empty many")
        void testFailEmptyMany() {
            assertThrows(RuntimeException.class, () -> assertParse(testee.manyToOneRuleParser, "()->id1\n", null));
        }

        @Test
        @DisplayName("fail on chained arrow operators")
        void testFailChain() {
            assertThrows(RuntimeException.class,
                    () -> assertParse(testee.manyToOneRuleParser, "(id1,id2)->id3->id4\n", null));
            assertThrows(RuntimeException.class,
                    () -> assertParse(testee.manyToOneRuleParser, "id1->(id2,id3)->id4\n", null));
        }

        @Test
        @DisplayName("parse definition with newline")
        void testNewLine() {
            assertParse(testee.manyToOneRuleParser, "(id1,id2)->id3\n",
                    new SyntaxTree.ManyToOneRule(Arrays.asList("id1", "id2"), "id3"));
        }

        @Test
        @DisplayName("parse definition without newline")
        void testNoNewLine() {
            assertParse(testee.manyToOneRuleParser, "(id2,id3)->id4",
                    new SyntaxTree.ManyToOneRule(Arrays.asList("id2", "id3"), "id4"));
        }

        @Test
        @DisplayName("parse definition with singleton")
        void testSingleton() {
            assertParse(testee.manyToOneRuleParser, "(id3)->id4",
                    new SyntaxTree.ManyToOneRule(Collections.singletonList("id3"), "id4"));
        }
    }

    @Test
    @DisplayName("rules parser should return all expected rules")
    public void rulesParserShouldReturnExpectedRules() {
        assertParse(testee.rulesParser, "id1->id2->id3\nid4-/->id5\nid6->id7\nid8->(id9,id10)\n(id11,id12)->id13",
                Arrays.asList(new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                        new SyntaxTree.NoDependentRule("id4", "id5"), new SyntaxTree.ChainDependencyRule("id6", "id7"),
                        new SyntaxTree.OneToManyRule("id8", Arrays.asList("id9", "id10")),
                        new SyntaxTree.ManyToOneRule(Arrays.asList("id11", "id12"), "id13")));
    }

    @Test
    public void rulesParserShouldIgnoreNewLinesExpectedRules() {
        assertParse(testee.rulesParser, "id1->id2->id3\n\n\nid6-/->id7\n", Arrays.asList(
                new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"), new SyntaxTree.NoDependentRule("id6", "id7")));
    }

    @Test
    public void moduleDefinitionsParserShouldReturnExpectedRules() {
        assertParse(testee.moduleDefinitions, "module1=\"asdfasdf\"\nmodule2=\"ee323  2343 sdf\"\nmodule3=\"\"\n",
                Arrays.asList(new SyntaxTree.ModuleDefinition("module1", "asdfasdf"),
                        new SyntaxTree.ModuleDefinition("module2", "ee323  2343 sdf"),
                        new SyntaxTree.ModuleDefinition("module3", "")));
    }

    @Test
    public void moduleDefinitionsParserShouldIgnoreNewlinesAndReturnExpectedRules() {
        assertParse(testee.moduleDefinitions, "module1=\"asdfasdf\"\n\n\nmodule3=\"\"\n",
                Arrays.asList(new SyntaxTree.ModuleDefinition("module1", "asdfasdf"),
                        new SyntaxTree.ModuleDefinition("module3", "")));
    }

    @Test
    public void prefixPreambleShouldParsePrefixColumnNewLineAndParseAdditionalNewLines() {
        assertParse(testee.prefixPreamble, "prefix:\n\n\n", null);
    }

    @Test
    @DisplayName("whitelist preamble should parse 'whitelist:' followed by newlines")
    void testWhiteListPreamble() {
        assertParse(testee.whiteListPreamble, "whitelist:\n\n\n\n", null);
    }

    @Test
    @DisplayName("blacklist preamble should parse 'blacklist:' followed by newlines")
    void testBlackListPreamble() {
        assertParse(testee.blackListPreamble, "blacklist:\n\n\n\n", null);
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
        assertParse(testee.prefixSection, "prefix:\n\n\n\n\"a regex\"\n\n\n\n", "a regex");
    }

    @Test
    @DisplayName("whiteListSection should parse preamble and list of regexes")
    void testWhiteListSection() {
        assertParse(testee.whiteListSection, "whitelist:\n\n\n\"a\",\"b\"\n\n\n\n", Arrays.asList("a", "b"));
    }

    @Test
    @DisplayName("blackListSection should parse preamble and list of regexes")
    void testBlackListSection() {
        assertParse(testee.blackListSection, "blacklist:\n\n\n\"c\",\"d\"\n\n\n\n", Arrays.asList("c", "d"));
    }

    @Test
    @DisplayName("whiteListSection should parse preamble and one regex")
    void testWhiteListSectionOne() {
        assertParse(testee.whiteListSection, "whitelist:\n\n\n\"a\"\n\n\n\n", Arrays.asList("a"));
    }

    @Test
    @DisplayName("blackListSection should parse preamble and list of regexes")
    void testBlackListSectionOne() {
        assertParse(testee.blackListSection, "blacklist:\n\n\n\"c\"\n\n\n\n", Arrays.asList("c"));
    }

    @Test
    public void modulesSectionShouldParsePreambleAndModulesDefinition() {
        assertParse(testee.modulesSection, "modules:\n\n\n\nm1=\"regex1.*\"\nm2=\"regex2+\"\n\n\n\n", Arrays.asList(
                new SyntaxTree.ModuleDefinition("m1", "regex1.*"), new SyntaxTree.ModuleDefinition("m2", "regex2+")));
    }

    @Test
    public void rulesSectionShouldParsePreambleAndRuleDefinitions() {
        assertParse(testee.rulesSection,
                "rules:\n\n\n\nid1->id2->id3\n\n\nid6-/->id7\n\nid8->id9\nid8->(id9,id10)\n(id11,id12)->id13\n",
                Arrays.asList(new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                        new SyntaxTree.NoDependentRule("id6", "id7"), new SyntaxTree.ChainDependencyRule("id8", "id9"),
                        new SyntaxTree.OneToManyRule("id8", Arrays.asList("id9", "id10")),
                        new SyntaxTree.ManyToOneRule(Arrays.asList("id11", "id12"), "id13")));
    }

    @Test
    public void rulesSectionShouldParsePreambleAndRuleWithEndOfFile() {
        assertParse(testee.rulesSection, "rules:\n\n\nida->idb",
                Collections.singletonList(new SyntaxTree.ChainDependencyRule("ida", "idb")));
    }

    @Test
    public void rulesSectionShouldParsePreambleAndNoDependecyRuleWithEndOfFile() {
        assertParse(testee.rulesSection, "rules:\n\n\nida->idb\nid2-/->id44", Arrays.asList(
                new SyntaxTree.ChainDependencyRule("ida", "idb"), new SyntaxTree.NoDependentRule("id2", "id44")));
    }

    @Test
    @DisplayName("rulesSection should parse preamble and one to many with end of file")
    public void testOneToManyEOF() {
        assertParse(testee.rulesSection, "rules:\n\n\nida->(idb)",
                Collections.singletonList(new SyntaxTree.OneToManyRule("ida", Collections.singletonList("idb"))));
    }

    @Test
    @DisplayName("rulesSection should parse preamble and many to one with end of file")
    public void testManyToOneEOF() {
        assertParse(testee.rulesSection, "rules:\n\n\n(id9)->id10",
                Collections.singletonList(new SyntaxTree.ManyToOneRule(Collections.singletonList("id9"), "id10")));
    }

    @Test
    @DisplayName("grammar should parse module and rules without prefix, whitelist and blacklist")
    public void testGrammarParseNoPrefixNoWhiteListNoBlackList() {
        assertParse(testee.grammar,
                "modules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid8->(id9,id10)\nid1->id2->id3\nid6-/->id7\n(id11,id12)->id13\nid8->id9\n",
                new SyntaxTree.Definition(
                        Arrays.asList(new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                                new SyntaxTree.ModuleDefinition("m2", "regex2+")),
                        Arrays.asList(new SyntaxTree.OneToManyRule("id8", Arrays.asList("id9", "id10")),
                                new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                                new SyntaxTree.NoDependentRule("id6", "id7"),
                                new SyntaxTree.ManyToOneRule(Arrays.asList("id11", "id12"), "id13"),
                                new SyntaxTree.ChainDependencyRule("id8", "id9"))));
    }

    @Test
    @DisplayName("grammar should parse prefix, modules and rules")
    public void testGrammarParsePrefix() {
        assertParse(testee.grammar,
                "prefix:\"the prefix\"modules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid1->id2->id3\nid6-/->id7\nid8->id9\n",
                new SyntaxTree.Definition(Optional.of("the prefix"),
                        Arrays.asList(new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                                new SyntaxTree.ModuleDefinition("m2", "regex2+")),
                        Arrays.asList(new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                                new SyntaxTree.NoDependentRule("id6", "id7"),
                                new SyntaxTree.ChainDependencyRule("id8", "id9"))));
    }

    @Test
    @DisplayName("grammar should parse blacklist, modules and rules")
    public void testGrammarParseBlacklist() {
        assertParse(testee.grammar,
                "blacklist:\"blacklist\"modules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid1->id2->id3\nid6-/->id7\nid8->id9\n",
                new SyntaxTree.Definition(Optional.empty(), Optional.empty(),
                        Optional.of(Collections.singletonList("blacklist")),
                        Arrays.asList(new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                                new SyntaxTree.ModuleDefinition("m2", "regex2+")),
                        Arrays.asList(new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                                new SyntaxTree.NoDependentRule("id6", "id7"),
                                new SyntaxTree.ChainDependencyRule("id8", "id9"))));
    }

    @Test
    @DisplayName("grammar should parse whitelist, modules and rules")
    public void testGrammarParseWhiteList() {
        assertParse(testee.grammar,
                "whitelist:\"whitelist\"modules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid1->id2->id3\nid6-/->id7\nid8->id9\n",
                new SyntaxTree.Definition(Optional.empty(), Optional.of(Collections.singletonList("whitelist")),
                        Optional.empty(),
                        Arrays.asList(new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                                new SyntaxTree.ModuleDefinition("m2", "regex2+")),
                        Arrays.asList(new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                                new SyntaxTree.NoDependentRule("id6", "id7"),
                                new SyntaxTree.ChainDependencyRule("id8", "id9"))));
    }

    @Test
    @DisplayName("grammar should parse prefix, whitelist, blacklist, modules and rules")
    public void testGrammarParseComplete() {
        assertParse(testee.grammar,
                "prefix:\"the prefix\"\nwhitelist:\"white1\",\"white2\"\nblacklist:\"black1\",\"black2\"\nmodules:\nm1=\"regex1.*\"\nm2=\"regex2+\"\nrules:\nid1->id2->id3\nid6-/->id7\nid8->id9\n",
                new SyntaxTree.Definition(Optional.of("the prefix"), Optional.of(Arrays.asList("white1", "white2")),
                        Optional.of(Arrays.asList("black1", "black2")),
                        Arrays.asList(new SyntaxTree.ModuleDefinition("m1", "regex1.*"),
                                new SyntaxTree.ModuleDefinition("m2", "regex2+")),
                        Arrays.asList(new SyntaxTree.ChainDependencyRule("id1", "id2", "id3"),
                                new SyntaxTree.NoDependentRule("id6", "id7"),
                                new SyntaxTree.ChainDependencyRule("id8", "id9"))));
    }

    @Test
    @DisplayName("parser should parse minimum definition (modules, rules) ignoring spaces and java comments")
    public void testBaseParserIgnore() {
        final InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("./example-def.txt"));
        assertThat(testee.parse(reader))
                .isEqualTo(
                        new SyntaxTree.Definition(
                                Arrays.asList(
                                        new SyntaxTree.ModuleDefinition("Core",
                                                Arrays.asList("com.pitest.highwheel.core.*",
                                                        "com.pitest.highwheel.core2.*")),
                                        new SyntaxTree.ModuleDefinition("Utils", "com.pitest.highwheel.utils.*"),
                                        new SyntaxTree.ModuleDefinition("Modules", "com.pitest.highwheel.modules.*"),
                                        new SyntaxTree.ModuleDefinition("Parser", "com.pitest.highwheel.parser.*")),
                                Arrays.asList(new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
                                        new SyntaxTree.NoDependentRule("Utils", "Core"),
                                        new SyntaxTree.NoDependentRule("Utils", "Parser"),
                                        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
                                        new SyntaxTree.ChainDependencyRule("Modules", "Utils"),
                                        new SyntaxTree.OneToManyRule("Modules", Arrays.asList("Core", "Utils")),
                                        new SyntaxTree.ManyToOneRule(Arrays.asList("Modules", "Core"), "Utils"))));
    }

    @Test
    @DisplayName("parser should parse definition (prefix, modules, rules) ignoring spaces and java comments")
    public void testPrefixBaseParserIgnore() {
        final InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("./example-def-with-prefix.txt"));
        assertThat(testee.parse(reader)).isEqualTo(new SyntaxTree.Definition(Optional.of("com.pitest.highwheel."),
                Arrays.asList(new SyntaxTree.ModuleDefinition("Core", Arrays.asList("core.*", "core2.*")),
                        new SyntaxTree.ModuleDefinition("Utils", "utils.*"),
                        new SyntaxTree.ModuleDefinition("Modules", "modules.*"),
                        new SyntaxTree.ModuleDefinition("Parser", "parser.*")),
                Arrays.asList(new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
                        new SyntaxTree.NoDependentRule("Utils", "Core"),
                        new SyntaxTree.NoDependentRule("Utils", "Parser"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Utils"),
                        new SyntaxTree.OneToManyRule("Modules", Arrays.asList("Core", "Utils")),
                        new SyntaxTree.ManyToOneRule(Arrays.asList("Modules", "Core"), "Utils"))));
    }

    @Test
    @DisplayName("parser should parse definition (whitelist, modules, rules) ignoring spaces and java comments")
    public void testWhitelistBaseParserIgnore() {
        final InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("./example-def-with-whitelist.txt"));
        assertThat(testee.parse(reader)).isEqualTo(new SyntaxTree.Definition(Optional.empty(),
                Optional.of(Arrays.asList("com.pitest.highwheel.", "something")), Optional.empty(),
                Arrays.asList(new SyntaxTree.ModuleDefinition("Core", Arrays.asList("core.*", "core2.*")),
                        new SyntaxTree.ModuleDefinition("Utils", "utils.*"),
                        new SyntaxTree.ModuleDefinition("Modules", "modules.*"),
                        new SyntaxTree.ModuleDefinition("Parser", "parser.*")),
                Arrays.asList(new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
                        new SyntaxTree.NoDependentRule("Utils", "Core"),
                        new SyntaxTree.NoDependentRule("Utils", "Parser"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Utils"),
                        new SyntaxTree.OneToManyRule("Modules", Arrays.asList("Core", "Utils")),
                        new SyntaxTree.ManyToOneRule(Arrays.asList("Modules", "Core"), "Utils"))));
    }

    @Test
    @DisplayName("parser should parse definition (blacklist, modules, rules) ignoring spaces and java comments")
    public void testBlacklistBaseParserIgnore() {
        final InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("./example-def-with-blacklist.txt"));
        assertThat(testee.parse(reader)).isEqualTo(new SyntaxTree.Definition(Optional.empty(), Optional.empty(),
                Optional.of(Arrays.asList("com.pitest.highwheel.", "something")),
                Arrays.asList(new SyntaxTree.ModuleDefinition("Core", Arrays.asList("core.*", "core2.*")),
                        new SyntaxTree.ModuleDefinition("Utils", "utils.*"),
                        new SyntaxTree.ModuleDefinition("Modules", "modules.*"),
                        new SyntaxTree.ModuleDefinition("Parser", "parser.*")),
                Arrays.asList(new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
                        new SyntaxTree.NoDependentRule("Utils", "Core"),
                        new SyntaxTree.NoDependentRule("Utils", "Parser"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Utils"),
                        new SyntaxTree.OneToManyRule("Modules", Arrays.asList("Core", "Utils")),
                        new SyntaxTree.ManyToOneRule(Arrays.asList("Modules", "Core"), "Utils"))));
    }

    @Test
    @DisplayName("parser should parse complete definition (prefix, whitelist blacklist, modules, rules) ignoring spaces and java comments")
    public void testCompleteBaseParserIgnore() {
        final InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("./example-def-with-alloptionals.txt"));
        assertThat(testee.parse(reader)).isEqualTo(new SyntaxTree.Definition(Optional.of("com.pitest.highwheel."),
                Optional.of(Arrays.asList("com.pitest.highwheel.", "foo")),
                Optional.of(Arrays.asList("com.pitest.highwheel.", "bar")),
                Arrays.asList(new SyntaxTree.ModuleDefinition("Core", Arrays.asList("core.*", "core2.*")),
                        new SyntaxTree.ModuleDefinition("Utils", "utils.*"),
                        new SyntaxTree.ModuleDefinition("Modules", "modules.*"),
                        new SyntaxTree.ModuleDefinition("Parser", "parser.*")),
                Arrays.asList(new SyntaxTree.ChainDependencyRule("Parser", "Core", "Utils"),
                        new SyntaxTree.NoDependentRule("Utils", "Core"),
                        new SyntaxTree.NoDependentRule("Utils", "Parser"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Core"),
                        new SyntaxTree.ChainDependencyRule("Modules", "Utils"),
                        new SyntaxTree.OneToManyRule("Modules", Arrays.asList("Core", "Utils")),
                        new SyntaxTree.ManyToOneRule(Arrays.asList("Modules", "Core"), "Utils"))));
    }

    private <T> void assertParse(Parser<T> parser, String source, T expected) {
        T actual = parser.from(testee.tp.tokeniser(), Parsers.EOF.skipMany()).parse(source);
        assertThat(actual).isEqualTo(expected);
    }
}
