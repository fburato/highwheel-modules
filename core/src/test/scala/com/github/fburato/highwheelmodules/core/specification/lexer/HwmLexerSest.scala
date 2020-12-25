package com.github.fburato.highwheelmodules.core.specification.lexer


import com.github.fburato.highwheelmodules.core.specification.ParserException
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.parsing.input.CharArrayReader

class HwmLexerSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  "HwmLexer" should {
    "parse identifiers" in {
      testee("test") shouldBe Right(List(Identifier("test")))
    }

    "parse string literals" in {
      testee(""""test"""") shouldBe Right(List(StringLiteral("test")))
    }

    "parse newlines" in {
      testee("\n") shouldBe Right(List(NewLine))
    }

    "remove backslash" in {
      testee("\\\n") shouldBe Right(List())
    }

    val keywordExpectations = Seq(
      ("modules", Keywords.Modules),
      ("rules", Keywords.Rules),
      ("prefix", Keywords.Prefix),
      ("whitelist", Keywords.Whitelist),
      ("blacklist", Keywords.Blacklist),
      ("mode", Keywords.Mode)
    )

    keywordExpectations.foreach {
      case (keyword, token) => s"parse keyword $keyword as $token" in {
        testee(keyword) shouldBe Right(List(token))
      }
    }

    val operatorExpectations = Seq(
      ("=", Operators.Equals),
      (",", Operators.Comma),
      ("->", Operators.Arrow),
      ("-/->", Operators.NotArrow),
      (":", Operators.DefinedAs),
      ("(", Operators.OpenParenthesis),
      (")", Operators.CloseParenthesis)
    )

    operatorExpectations.foreach {
      case (keyword, token) => s"parse operator $keyword as $token" in {
        testee(keyword) shouldBe Right(List(token))
      }
    }

    "parse sequence of tokens" in {
      testee(
        """modules:test rules "\dfer" ( () mode blacklist hello
          |mode,->\
          |
          |
          |-/->""".stripMargin) shouldBe Right(List(Keywords.Modules, Operators.DefinedAs, Identifier("test"), Keywords.Rules,
        StringLiteral("\\dfer"), Operators.OpenParenthesis, Operators.OpenParenthesis, Operators.CloseParenthesis, Keywords.Mode,
        Keywords.Blacklist, Identifier("hello"), NewLine, Keywords.Mode, Operators.Comma, Operators.Arrow, NewLine, Operators.NotArrow
      ))
    }

    "collapse newlines" in {
      testee(
        """mode
          |
          |
          |
          |""".stripMargin) shouldBe Right(List(Keywords.Mode, NewLine))
    }

    "ignore comments" in {
      val result = testee(
        """mode blacklist : // a comment
          |// another comment
          |-> \
          |// comment with \
          |-/-> test
          |""".stripMargin)
      result shouldBe Right(List(
        Keywords.Mode, Keywords.Blacklist, Operators.DefinedAs,
        NewLine, NewLine, Operators.Arrow, NewLine, Operators.NotArrow, Identifier("test"),
        NewLine
      ))
    }

    "fail if string literal is not closed" in {
      testee(""""test stest""") match {
        case Right(_) => fail("should not succeed")
        case Left(_) => ()
      }
    }
  }

  def testee(s: String): Either[ParserException, List[HwmToken]] = HwmLexer(new CharArrayReader(s.toCharArray))
}
