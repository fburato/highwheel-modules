package com.github.fburato.highwheelmodules.core.specification.parsers;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public void shouldNotTokeniseOtherSpecialCharacters() {
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
    public void keywordsShouldBeTokenised() {
        parser.parse("modules");
        parser.parse("rules");
        parser.parse("prefix");
        parser.parse("whitelist");
        parser.parse("blacklist");
    }

    @Test
    public void identifiersShouldBeTokenised() {
        parser.parse("foobar");
        parser.parse("_barfoo_");
        parser.parse("A12SDss__sdf");
    }

    @Test
    public void doubleQuotedStringsShouldBeTokenised() {
        parser.parse("\"asdf\"");
        parser.parse("\"something that would not be normally parsed ---a--cc''..s12312312\\\"\"");
    }

    @Test
    public void equalsShouldParseEqual() {
        assertParse(testee.equals(), "=");
    }

    @Test
    public void commaShouldParseComma() {
        assertParse(testee.comma(), ",");
    }

    @Test
    public void arrowShouldParseArrow() {
        assertParse(testee.arrow(), "->");
    }

    @Test
    public void notArrowShouldParseNotArrow() {
        assertParse(testee.notArrow(), "-/->");
    }

    @Test
    public void definedAsShouldParseColumn() {
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
    public void modulesPreambleShouldParseModulesKeyword() {
        assertParse(testee.modulesPreamble(), "modules");
    }

    @Test
    public void rulesPreambleShouldParseRulesKeyword() {
        assertParse(testee.rulesPreamble(), "rules");
    }

    @Test
    public void prefixPreambleShouldParsePrefixKeyword() {
        assertParse(testee.prefixPreamble(), "prefix");
    }

    @Test
    @DisplayName("whitelist preamble should parse whitelist keyword")
    public void testWhiteListPreamble() {
        assertParse(testee.whiteListPreamble(), "whitelist");
    }

    @Test
    @DisplayName("blacklist preamble should parse blacklist keyword")
    public void testBlackListPreamble() {
        assertParse(testee.blackListPreamble(), "blacklist");
    }

    @Test
    public void newLineShouldParseNewLine() {
        assertParse(testee.newLine(), "\n");
    }

    @Test
    public void moduleNameShouldParseIdentifiers() {
        assertParse(testee.moduleName(), "_an_identifier");
    }

    @Test
    public void moduleRegexShouldParseDoubleQuotedStringLiteral() {
        assertParse(testee.stringLiteral(), "\"asdfasdf121123  sdfwe{{\"");
    }

    @Test
    public void moduleRegexShouldFailOnNotTerminatedDoubleQuotedStringLiteral() {
        assertThrows(RuntimeException.class, () -> assertParse(testee.stringLiteral(), "\"asdfasdf121123  sdfwe{{"));
    }

    public void assertParse(Parser<?> p, String source) {
        p.from(parser, Parsers.EOF.skipMany()).parse(source);
    }
}
