package com.github.fburato.highwheelmodules.core.specification.parsers;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.error.ParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("TerminalParser")
public class TerminalParserTest {

    private final TerminalParser testee = new TerminalParser();
    private final Parser<?> parser = testee.tokeniser();

    @Test
    @DisplayName("operators should be tokenised")
    public void testOperatorsTokenised() {
        parser.parse("=");
        parser.parse("->");
        parser.parse("-/->");
        parser.parse("\n");
        parser.parse(":");
        parser.parse(",");
        parser.parse(")");
        parser.parse("(");
    }

    @Test
    @DisplayName("should not tokenise other special characters")
    public void testFailOtherSpecial() {
        final String[] otherSpecial = new String[] { ">", "<->", ".", ";" };
        for (String other : otherSpecial) {
            boolean exceptionThrown = false;
            try {
                parser.parse(other);
            } catch (Exception e) {
                exceptionThrown = true;
            }
            assertThat(exceptionThrown).isTrue().describedAs(other + " was parsed but it shouldn't have been");
        }
    }

    @Test
    @DisplayName("should tokenise keywords")
    public void testTokeniseKeywords() {
        parser.parse("modules");
        parser.parse("rules");
        parser.parse("prefix");
        parser.parse("whitelist");
        parser.parse("blacklist");
        parser.parse("mode");
    }

    @Test
    @DisplayName("should tokenise identifiers")
    public void testIdentifierTokenise() {
        parser.parse("foobar");
        parser.parse("_barfoo_");
        parser.parse("A12SDss__sdf");
    }

    @Test
    @DisplayName("should tokenise quoted strings")
    public void testTokeniseQuotes() {
        parser.parse("\"asdf\"");
        parser.parse("\"something that would not be normally parsed ---a--cc''..s12312312\\\"\"");
    }

    @Test
    @DisplayName("equals should parse '='")
    public void testEqual() {
        assertParse(testee.equals(), "=");
    }

    @Test
    @DisplayName("comma should parse ','")
    public void testComma() {
        assertParse(testee.comma(), ",");
    }

    @Test
    @DisplayName("arrow should parse '->'")
    public void testArrow() {
        assertParse(testee.arrow(), "->");
    }

    @Test
    @DisplayName("notArrow should parse '-/->'")
    public void testNotArrow() {
        assertParse(testee.notArrow(), "-/->");
    }

    @Test
    @DisplayName("definedAs should parse ':'")
    public void testDefinedAs() {
        assertParse(testee.definedAs(), ":");
    }

    @Test
    @DisplayName("openParen should parse '('")
    void testOpenParenParse() {
        assertParse(testee.openParen(), "(");
    }

    @Test
    @DisplayName("closedParen should parse ')'")
    void testClosedParenParse() {
        assertParse(testee.closedParen(), ")");
    }

    @Test
    @DisplayName("modulesPreamble should parse 'modules'")
    public void testModulesPreamble() {
        assertParse(testee.modulesPreamble(), "modules");
    }

    @Test
    @DisplayName("rulesPreamble should parse 'rules'")
    public void testRulesPreamble() {
        assertParse(testee.rulesPreamble(), "rules");
    }

    @Test
    @DisplayName("prefixPreamble should parse 'prefix'")
    public void testPrefixPreamble() {
        assertParse(testee.prefixPreamble(), "prefix");
    }

    @Test
    @DisplayName("whitelist preamble should parse 'whitelist'")
    public void testWhiteListPreamble() {
        assertParse(testee.whiteListPreamble(), "whitelist");
    }

    @Test
    @DisplayName("blacklist preamble should parse 'blacklist'")
    public void testBlackListPreamble() {
        assertParse(testee.blackListPreamble(), "blacklist");
    }

    @Test
    @DisplayName("mode preamble should parse 'mode'")
    public void testModePreamble() {
        assertParse(testee.modePreamble(), "mode");
    }

    @Test
    @DisplayName("newLine should parse '\\n'")
    public void newLineShouldParseNewLine() {
        assertParse(testee.newLine(), "\n");
    }

    @Test
    @DisplayName("newline label should be different from literal newline")
    public void testNewlineLabel() {
        assertThatCode(() -> assertParse(testee.newLine(), "")).hasMessageContaining("\\n (newline)");
    }

    @Test
    @DisplayName("moduleName should parse identifiers")
    public void testModuleName() {
        assertParse(testee.moduleName(), "_an_identifier");
    }

    @Test
    @DisplayName("stringLiteral should parse quoted string")
    public void testStringLiteral() {
        assertParse(testee.stringLiteral(), "\"asdfasdf121123  sdfwe{{\"");
    }

    @Test
    @DisplayName("mode should parse identifier")
    void testModeIdentifier() {
        assertParse(testee.mode(), "STRICT");
    }

    @Test
    @DisplayName("mode should not parse with spaces")
    void testModeNoSpaces() {
        assertThrows(ParserException.class, () -> assertParse(testee.mode(), "test with space"));
    }

    @Test
    @DisplayName("stringLiteral should fail to parse not quote terminated string")
    public void moduleRegexShouldFailOnNotTerminatedDoubleQuotedStringLiteral() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.stringLiteral(), "\"asdfasdf121123  sdfwe{{"));
    }

    public void assertParse(Parser<?> p, String source) {
        p.from(parser, Parsers.EOF.skipMany()).parse(source);
    }
}
