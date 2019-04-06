package com.github.fburato.highwheelmodules.core.specification.parsers;

import org.jparsec.*;

final class TerminalParser {
    private final String[] operators = { "=", "\n", ":", "->", "-/->", "," };

    private final String MODULES_KEYWORD = "modules";
    private final String RULES_KEYWORD = "rules";
    private final String PREFIX_KEYWORD = "prefix";
    private final String WHITELIST_KEYWORD = "whitelist";
    private final String BLACKLIST_KEWYWORD = "blacklist";

    private final Terminals terminals = Terminals.operators(operators).words(Scanners.IDENTIFIER)
            .keywords(MODULES_KEYWORD, RULES_KEYWORD, PREFIX_KEYWORD, WHITELIST_KEYWORD, BLACKLIST_KEWYWORD).build();

    private final Parser<?> tokeniser = Parsers.or(terminals.tokenizer(),
            Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER);

    private Parser<Token> term(String name) {
        return terminals.token(name);
    }

    private final Parser<Token> equals = term("=");

    public Parser<Token> equals() {
        return this.equals;
    }

    private final Parser<Token> comma = term(",");

    public Parser<Token> comma() {
        return comma;
    }

    private final Parser<Token> arrow = term("->");

    public Parser<Token> arrow() {
        return this.arrow;
    }

    private final Parser<Token> notArrow = term("-/->");

    public Parser<Token> notArrow() {
        return this.notArrow;
    }

    private final Parser<Token> definedAs = term(":");

    public Parser<Token> definedAs() {
        return this.definedAs;
    }

    private final Parser<Token> newLine = term("\n");

    public Parser<Token> newLine() {
        return this.newLine;
    }

    private final Parser<Token> prefixPreamble = term(PREFIX_KEYWORD);

    public Parser<Token> prefixPreamble() {
        return this.prefixPreamble;
    }

    private final Parser<Token> whiteListPreamble = term(WHITELIST_KEYWORD);

    public Parser<Token> whiteListPreamble() {
        return whiteListPreamble;
    }

    private final Parser<Token> blackListPreamble = term(BLACKLIST_KEWYWORD);

    public Parser<Token> blackListPreamble() {
        return blackListPreamble;
    }

    private final Parser<Token> modulesPreamble = term(MODULES_KEYWORD);

    public Parser<Token> modulesPreamble() {
        return this.modulesPreamble;
    }

    private final Parser<Token> rulesPreamble = term(RULES_KEYWORD);

    public Parser<Token> rulesPreamble() {
        return this.rulesPreamble;
    }

    private final Parser<String> moduleName = Terminals.Identifier.PARSER;

    public Parser<String> moduleName() {
        return this.moduleName;
    }

    private final Parser<String> moduleRegex = Terminals.StringLiteral.PARSER;

    public Parser<String> stringLiteral() {
        return this.moduleRegex;
    }

    public TerminalParser() {
    }

    public Parser<?> tokeniser() {
        return tokeniser;
    }
}
