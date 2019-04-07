package com.github.fburato.highwheelmodules.core.specification.parsers;

import com.github.fburato.highwheelmodules.core.specification.SyntaxTree;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DefinitionParser {

    final TerminalParser tp = new TerminalParser();

    private final Parser<String> commaRegex = Parsers.sequence(tp.comma(), tp.stringLiteral(),
            (Token t, String s) -> s);

    final Parser<SyntaxTree.ModuleDefinition> moduleDefinitionParser = Parsers.sequence(tp.moduleName(), tp.equals(),
            tp.stringLiteral(), commaRegex.many(), tp.newLine(),
            (String s, Token token, String s2, List<String> list, Token token2) -> {
                final List<String> result = new ArrayList<>(list.size() + 1);
                result.add(s2);
                result.addAll(list);
                return new SyntaxTree.ModuleDefinition(s, result);
            });

    final Parser<SyntaxTree.ChainDependencyRule> chainDependencyRuleParser = Parsers.sequence(tp.moduleName(),
            Parsers.sequence(tp.arrow(), tp.moduleName(), (Token t, String s) -> s).many1(),
            Parsers.or(tp.newLine(), Parsers.EOF), (String s, List<String> strings, Object t) -> {
                final List<String> result = new ArrayList<>(strings.size() + 1);
                result.add(s);
                result.addAll(strings);
                return new SyntaxTree.ChainDependencyRule(result);
            });

    final Parser<SyntaxTree.NoDependentRule> noDependecyRuleParser = Parsers.sequence(tp.moduleName(), tp.notArrow(),
            tp.moduleName(), Parsers.or(tp.newLine(), Parsers.EOF),
            (String s, Token token, String s2, Object token2) -> new SyntaxTree.NoDependentRule(s, s2));

    private final Parser<String> commaIdentifier = Parsers.sequence(tp.comma(), tp.moduleName(),
            (Token v, String id) -> id);

    private final Parser<List<String>> identifierList = Parsers.sequence(tp.moduleName(), commaIdentifier.many(),
            (String id, List<String> ids) -> {
                final List<String> res = new ArrayList<>(ids.size() + 1);
                res.add(id);
                res.addAll(ids);
                return res;
            });

    final Parser<SyntaxTree.OneToManyRule> oneToManyRuleParser = Parsers.sequence(tp.moduleName(), tp.arrow(),
            tp.openParen(), identifierList, tp.closedParen(), Parsers.or(tp.newLine(), Parsers.EOF),
            (String id, Token arrow, Token oP, List<String> ids, Token cP,
                    Object end) -> new SyntaxTree.OneToManyRule(id, ids));

    final Parser<SyntaxTree.ManyToOneRule> manyToOneRuleParser = Parsers.sequence(tp.openParen(), identifierList,
            tp.closedParen(), tp.arrow(), tp.moduleName(), Parsers.or(tp.newLine(), Parsers.EOF),
            (Token op, List<String> ids, Token cP, Token arrow, String id,
                    Object eof) -> new SyntaxTree.ManyToOneRule(ids, id));

    private final Parser<SyntaxTree.Rule> anyRuleParser = Parsers.or(chainDependencyRuleParser, noDependecyRuleParser,
            oneToManyRuleParser, manyToOneRuleParser);

    final Parser<List<SyntaxTree.Rule>> rulesParser = Parsers
            .sequence(anyRuleParser, tp.newLine().many(), (SyntaxTree.Rule rule, List<Token> tokens) -> rule).many();

    final Parser<List<SyntaxTree.ModuleDefinition>> moduleDefinitions = Parsers.sequence(moduleDefinitionParser,
            tp.newLine().many(), (SyntaxTree.ModuleDefinition moduleDefinition, List<Token> tokens) -> moduleDefinition)
            .many();

    final Parser<Void> prefixPreamble = Parsers.sequence(tp.prefixPreamble(), tp.definedAs(), tp.newLine().many(),
            (Token t1, Token t2, List<Token> nl) -> null);

    final Parser<Void> whiteListPreamble = Parsers.sequence(tp.whiteListPreamble(), tp.definedAs(), tp.newLine().many(),
            (Token t1, Token t2, List<Token> nl) -> null);

    final Parser<Void> blackListPreamble = Parsers.sequence(tp.blackListPreamble(), tp.definedAs(), tp.newLine().many(),
            (Token t1, Token t2, List<Token> nl) -> null);

    final Parser<Void> modulesPreamble = Parsers.sequence(tp.modulesPreamble(), tp.definedAs(), tp.newLine().many(),
            (Token token, Token token2, List<Token> d) -> null);

    final Parser<Void> rulesPreamble = Parsers.sequence(tp.rulesPreamble(), tp.definedAs(), tp.newLine().many(),
            (Token token, Token token2, List<Token> d) -> null);

    final Parser<String> prefixSection = Parsers.sequence(prefixPreamble, tp.stringLiteral(), tp.newLine().many(),
            (Void preamble, String literal, List<Token> d) -> literal);

    private final Parser<List<String>> stringLiteralsList = Parsers.sequence(tp.stringLiteral(), commaRegex.many(),
            (String l, List<String> ls) -> {
                final List<String> list = new ArrayList<>();
                list.add(l);
                list.addAll(ls);
                return list;
            });

    final Parser<List<String>> whiteListSection = Parsers.sequence(whiteListPreamble, stringLiteralsList,
            tp.newLine().many(), (Void preamble, List<String> rs, List<Token> d) -> rs);

    final Parser<List<String>> blackListSection = Parsers.sequence(blackListPreamble, stringLiteralsList,
            tp.newLine().many(), (Void preamble, List<String> rs, List<Token> d) -> rs);

    final Parser<List<SyntaxTree.ModuleDefinition>> modulesSection = Parsers.sequence(modulesPreamble,
            moduleDefinitions);

    final Parser<List<SyntaxTree.Rule>> rulesSection = Parsers.sequence(rulesPreamble, rulesParser);

    final Parser<SyntaxTree.Definition> grammar = Parsers.sequence(prefixSection.asOptional(),
            whiteListSection.asOptional(), blackListSection.asOptional(), modulesSection, rulesSection,
            SyntaxTree.Definition::new);

    private static Parser<Void> javacomment = Scanners.JAVA_LINE_COMMENT;

    private static final Parser<Void> ignore = Parsers.or(Scanners.among(" \t"), javacomment);

    public SyntaxTree.Definition parse(Readable readable) {
        try {
            return grammar.from(tp.tokeniser(), ignore.skipMany()).parse(readable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
