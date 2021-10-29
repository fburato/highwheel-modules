package com.github.fburato.highwheelmodules.core.specification

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

package object lexer {

  sealed trait HwmToken

  case class Identifier(str: String) extends HwmToken

  case class StringLiteral(str: String) extends HwmToken

  case object NewLine extends HwmToken

  case object Separator extends HwmToken

  case object Backslash extends HwmToken

  case object Comment extends HwmToken

  object Keywords {

    case object Modules extends HwmToken

    case object Rules extends HwmToken

    case object Prefix extends HwmToken

    case object Whitelist extends HwmToken

    case object Blacklist extends HwmToken

    case object Mode extends HwmToken

  }

  object Operators {

    case object Equals extends HwmToken

    case object Comma extends HwmToken

    case object Arrow extends HwmToken

    case object NotArrow extends HwmToken

    case object DefinedAs extends HwmToken

    case object OpenParenthesis extends HwmToken

    case object CloseParenthesis extends HwmToken

  }

  object HwmLexerDemo extends RegexParsers {
    override def skipWhitespace: Boolean = true

    override val whiteSpace: Regex = "[ \t\r\f]+".r

    def identifier: Parser[Identifier] = "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => Identifier(str) }

    def backslash: Parser[HwmToken] = "(\\\\\r?\n)+".r ^^ { _ => Backslash }

    def newline: Parser[HwmToken] = "(?m)(\r?\n)+".r ^^ { _ => NewLine }

    def literal: Parser[StringLiteral] =
      """"[^"]*"""".r ^^ { str =>
        val content = str.substring(1, str.length - 1)
        StringLiteral(content)
      }

    def tokens: Parser[List[HwmToken]] = phrase(rep1(identifier | newline | backslash)) ^^ {
      _.filterNot(t => t == Backslash)
    }

    def apply(code: String): Either[Exception, List[HwmToken]] = parse(tokens, code) match {
      case NoSuccess(msg, next) => Left(new Exception(msg + next))
      case Success(result, _)   => Right(result)
      case Failure(msg, next)   => Left(new Exception(msg + next))
      case Error(msg, next)     => Left(new Exception(msg + next))
    }
  }

  def main(args: Array[String]): Unit = {
    val string = """"""
    val regex = "[ \t\r\f]+".r
    println(regex.matches(string))
  }
}
