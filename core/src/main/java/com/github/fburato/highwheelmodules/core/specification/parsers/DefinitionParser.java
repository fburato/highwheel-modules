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

  private final Parser<String> commaRegex = Parsers.sequence(tp.comma(), tp.stringLiteral(), (Token t, String s) -> s);

  final Parser<SyntaxTree.ModuleDefinition> moduleDefinitionParser =
      Parsers.sequence(tp.moduleName(), tp.equals(), tp.stringLiteral(), commaRegex.many(), tp.newLine(),
          (String s, Token token, String s2, List<String> list, Token token2) -> {
            final List<String> result = new ArrayList<>(list.size() + 1);
            result.add(s2);
            result.addAll(list);
            return new SyntaxTree.ModuleDefinition(s, result);
          });

  final Parser<SyntaxTree.ChainDependencyRule> chainDependencyRuleParser =
      Parsers.sequence(tp.moduleName(), Parsers.sequence(tp.arrow(), tp.moduleName(), (Token t, String s) -> s).many1(),
          Parsers.or(tp.newLine(),Parsers.EOF),
          (String s, List<String> strings, Object t) -> {
            final List<String> result = new ArrayList<>(strings.size() + 1);
            result.add(s);
            result.addAll(strings);
            return new SyntaxTree.ChainDependencyRule(result);
          });

  final Parser<SyntaxTree.NoDependentRule> noDependecyRuleParser =
      Parsers.sequence(tp.moduleName(), tp.notArrow(), tp.moduleName(), tp.newLine(),
          (String s, Token token, String s2, Token token2) -> new SyntaxTree.NoDependentRule(s, s2));

  private final Parser<SyntaxTree.Rule> anyRuleParser = Parsers.or(chainDependencyRuleParser, noDependecyRuleParser);

  final Parser<List<SyntaxTree.Rule>> rulesParser = Parsers.sequence(anyRuleParser, tp.newLine().many(),
      (SyntaxTree.Rule rule, List<Token> tokens) -> rule).many();

  final Parser<List<SyntaxTree.ModuleDefinition>> moduleDefinitions =
      Parsers.sequence(moduleDefinitionParser, tp.newLine().many(),
          (SyntaxTree.ModuleDefinition moduleDefinition, List<Token> tokens) -> moduleDefinition).many();

  final Parser<Void> prefixPreamble = Parsers.sequence(tp.prefixPreamble(), tp.definedAs(), tp.newLine().many(),
      (Token t1, Token t2, List<Token> nl) -> null
      );

  final Parser<Void> modulesPreamble = Parsers.sequence(tp.modulesPreamble(), tp.definedAs(), tp.newLine().many(),
      (Token token, Token token2, List<Token> d) -> null);

  final Parser<Void> rulesPreamble = Parsers.sequence(tp.rulesPreamble(), tp.definedAs(), tp.newLine().many(),
      (Token token, Token token2, List<Token> d) -> null);

  final Parser<Optional<String>> prefixSection = Parsers.sequence(prefixPreamble, tp.stringLiteral(), tp.newLine().many(),
      (Void preamble, String literal, List<Token> d) -> Optional.of(literal));

  final Parser<List<SyntaxTree.ModuleDefinition>> modulesSection = Parsers.sequence(modulesPreamble, moduleDefinitions);

  final Parser<List<SyntaxTree.Rule>> rulesSection = Parsers.sequence(rulesPreamble, rulesParser);

  final Parser<SyntaxTree.Definition> grammar = Parsers.sequence(prefixSection.optional(Optional.empty()),
      modulesSection, rulesSection, SyntaxTree.Definition::new);

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
