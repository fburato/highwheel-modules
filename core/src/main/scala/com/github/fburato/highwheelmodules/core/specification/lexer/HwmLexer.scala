package com.github.fburato.highwheelmodules.core.specification.lexer

import com.github.fburato.highwheelmodules.core.specification.ParserException

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.Reader

object HwmLexer extends RegexParsers {

  override def skipWhitespace: Boolean = true

  override val whiteSpace: Regex = "[ \t\r\f]+".r

  def keywordParser(keyword: String, token: HwmToken): Parser[HwmToken] =
    keyword.r ^^ { _ => token }

  private def identifier: Parser[Identifier] = "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => Identifier(str) }

  private def backslash: Parser[HwmToken] = "(\\\\\r?\n)+".r ^^ { _ => Backslash }

  private def newline: Parser[HwmToken] = "(?m)(\r?\n)+".r ^^ { _ => NewLine }

  private def comment: Parser[HwmToken] = "//[^\n]+".r ^^ { _ => Comment }

  private def literal: Parser[StringLiteral] =
    """"[^"]*"""".r ^^ { str =>
      val content = str.substring(1, str.length - 1)
      StringLiteral(content)
    }

  def apply(in: Reader[Char]): Either[ParserException, List[HwmToken]] =
    parse(phrase(rep1(
      keywordParser("modules", Keywords.Modules) |
        keywordParser("rules", Keywords.Rules) |
        keywordParser("prefix", Keywords.Prefix) |
        keywordParser("whitelist", Keywords.Whitelist) |
        keywordParser("blacklist", Keywords.Blacklist) |
        keywordParser("mode", Keywords.Mode) |
        keywordParser("=", Operators.Equals) |
        keywordParser(",", Operators.Comma) |
        keywordParser("->", Operators.Arrow) |
        keywordParser("-/->", Operators.NotArrow) |
        keywordParser(":", Operators.DefinedAs) |
        keywordParser("\\(", Operators.OpenParenthesis) |
        keywordParser("\\)", Operators.CloseParenthesis) |
        newline |
        backslash |
        comment |
        identifier |
        literal)), in) match {
      case Success(r, _) => Right(r filterNot (t => t == Comment || t == Backslash))
      case NoSuccess(msg, next) => Left(ParserException(s"$msg at ${next.pos}"))
    }
}
