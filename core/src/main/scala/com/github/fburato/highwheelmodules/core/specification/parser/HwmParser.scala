package com.github.fburato.highwheelmodules.core.specification.parser

import com.github.fburato.highwheelmodules.core.specification._
import com.github.fburato.highwheelmodules.core.specification.lexer._

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{Position, Reader}

object HwmParser extends Parsers {

  private class HwmReader(tokens: Seq[HwmToken]) extends Reader[HwmToken] {
    override def first: HwmToken = tokens.head

    override def rest: Reader[HwmToken] = new HwmReader(tokens.tail)

    override def pos: Position = new Position {
      override def line: Int = 1

      override def column: Int = tokens.size

      override protected def lineContents: String = first.toString
    }

    override def atEnd: Boolean = tokens.isEmpty
  }

  override type Elem = HwmToken

  private val definedAs = accept(":", { case Operators.DefinedAs => () })
  private val literal = accept("string literal", { case StringLiteral(str) => str })
  private val newLine = accept("new line", { case NewLine => () })
  private val newlines = rep1(newLine)
  private val identifier = accept("identifier", { case Identifier(str) => str })
  private val comma = accept(",", { case Operators.Comma => () })
  private val arrow = accept("->", { case Operators.Arrow => () })
  private val notArrow = accept("-/->", { case Operators.NotArrow => () })
  private val openParenthesis = accept("(", { case Operators.OpenParenthesis => () })
  private val closeParenthesis = accept(")", { case Operators.CloseParenthesis => () })
  private val literals = rep1sep(literal, comma)
  private val identifiers = rep1sep(identifier, comma)

  private[parser] def prefixParser: Parser[String] = {
    val prefix = accept("prefix", { case Keywords.Prefix => () })
    (prefix ~ definedAs ~ rep(newLine) ~ literal ~ newlines) ^^ { case _ ~ _ ~ _ ~ prefix ~ _ =>
      prefix
    }
  }

  private[parser] def whitelistParser: Parser[List[String]] = {
    val whitelist = accept("whitelist", { case Keywords.Whitelist => () })
    (whitelist ~ definedAs ~ rep(newLine) ~ literals ~ newlines) ^^ { case _ ~ _ ~ _ ~ l ~ _ =>
      l
    }
  }

  private[parser] def blacklistParser: Parser[List[String]] = {
    val blacklist = accept("blacklist", { case Keywords.Blacklist => () })
    (blacklist ~ definedAs ~ rep(newLine) ~ literals ~ newlines) ^^ { case _ ~ _ ~ _ ~ l ~ _ =>
      l
    }
  }

  private[parser] def modeParser: Parser[String] = {
    val mode = accept("mode", { case Keywords.Mode => () })
    (mode ~ definedAs ~ rep(newLine) ~ identifier ~ newlines) ^^ { case _ ~ _ ~ _ ~ id ~ _ =>
      id
    }
  }

  private[parser] def moduleDefinitionParser: Parser[ModuleDefinition] = {
    val equals = accept("equals", { case Operators.Equals => () })
    (identifier ~ equals ~ literals ~ newLine) ^^ { case i ~ _ ~ l ~ _ =>
      ModuleDefinition(i, l)
    }
  }

  private[parser] def chainDependencyRuleParser: Parser[ChainDependencyRule] = {
    val chainedDependency = (arrow ~ identifier) ^^ { case _ ~ l =>
      l
    }
    (identifier ~ rep1(chainedDependency)) ^^ { case first ~ rest =>
      ChainDependencyRule(first :: rest)
    }
  }

  private[parser] def noDependencyRuleParser: Parser[NoDependentRule] = {
    (identifier ~ notArrow ~ identifier) ^^ { case id1 ~ _ ~ id2 =>
      NoDependentRule(id1, id2)
    }
  }

  private[parser] def oneToManyRuleParser: Parser[OneToManyRule] = {
    (identifier ~ arrow ~ openParenthesis ~ identifiers ~ closeParenthesis) ^^ {
      case id1 ~ _ ~ _ ~ ids ~ _ => OneToManyRule(id1, ids)
    }
  }

  private[parser] def manyToOneRuleParser: Parser[ManyToOneRule] = {
    (openParenthesis ~ identifiers ~ closeParenthesis ~ arrow ~ identifier) ^^ {
      case _ ~ ids ~ _ ~ _ ~ id1 => ManyToOneRule(ids, id1)
    }
  }

  private[parser] def interpret[T](p: Parser[T], input: Input): Either[String, T] =
    p(input) match {
      case Success(r, _)     => Right(r)
      case NoSuccess(msg, _) => Left(msg)
    }

  private val definitionParser = {
    val moduleKeywordParser = accept("modules", { case Keywords.Modules => () })
    val rulesKeywordParser = accept("rules", { case Keywords.Rules => () })
    val modulesPreambleParser = (moduleKeywordParser ~ definedAs ~ newlines) ^^ { case _ ~ _ ~ _ =>
      ()
    }
    val rulesPreambleParser = (rulesKeywordParser ~ definedAs ~ newlines) ^^ { case _ ~ _ ~ _ =>
      ()
    }
    val allRulesParser = repsep(
      chainDependencyRuleParser | noDependencyRuleParser | oneToManyRuleParser | manyToOneRuleParser,
      newlines
    )
    (opt(prefixParser) ~
      opt(whitelistParser) ~
      opt(blacklistParser) ~
      opt(modeParser) ~
      modulesPreambleParser ~
      rep(moduleDefinitionParser) ~
      rulesPreambleParser ~
      allRulesParser ~ rep(newLine)) ^^ {
      case prefix ~ whitelist ~ blacklist ~ mode ~ _ ~ modules ~ _ ~ rules ~ _ =>
        Definition(prefix, whitelist, blacklist, mode, modules, rules)
    }
  }

  def parse(tokens: Seq[HwmToken]): Either[ParserException, Definition] = {
    val reader = new HwmReader(tokens)
    phrase(definitionParser)(reader) match {
      case NoSuccess(msg, next) =>
        Left(ParserException(s"$msg at ${next.pos}, content ${next.first}"))
      case Success(result, _) => Right(result)
    }
  }
}
