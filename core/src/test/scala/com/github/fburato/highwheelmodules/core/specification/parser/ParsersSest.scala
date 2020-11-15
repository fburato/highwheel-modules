package com.github.fburato.highwheelmodules.core.specification.parser

import com.github.fburato.highwheelmodules.core.specification._
import com.github.fburato.highwheelmodules.core.specification.lexer._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.io.Source
import scala.util.parsing.input._

class ParsersSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  "prefix parser" should {
    "parse expected format" in {
      val stringLiteral = "literal"
      run(HwmParser.prefixParser, List(Keywords.Prefix, Operators.DefinedAs, StringLiteral(stringLiteral), NewLine)) shouldBe Right(stringLiteral)
    }

    "fail on missing prefix" in {
      run(HwmParser.prefixParser, List(Operators.DefinedAs, StringLiteral(""), NewLine)).isLeft shouldBe true
    }

    "fail on missing column" in {
      run(HwmParser.prefixParser, List(Keywords.Prefix, StringLiteral(""), NewLine)).isLeft shouldBe true
    }

    "fail on missing string literal" in {
      run(HwmParser.prefixParser, List(Keywords.Prefix, Operators.DefinedAs, NewLine)).isLeft shouldBe true
    }

    "fail on missing new line" in {
      run(HwmParser.prefixParser, List(Keywords.Prefix, Operators.DefinedAs, StringLiteral(""))).isLeft shouldBe true
    }
  }

  "whitelist parser" should {
    "parse expected format with one literal" in {
      val literal = "literal"
      run(HwmParser.whitelistParser, List(Keywords.Whitelist, Operators.DefinedAs, StringLiteral(literal), NewLine)) shouldBe Right(List(literal))
    }

    "parse expected format with multiple literals" in {
      val literal1 = "literal1"
      val literal2 = "literal2"
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        Operators.DefinedAs,
        StringLiteral(literal1), Operators.Comma, StringLiteral(literal2),
        NewLine)) shouldBe Right(List(literal1, literal2))
    }

    "fail on missing whitelist" in {
      run(HwmParser.whitelistParser, List(Operators.DefinedAs,
        StringLiteral(""), Operators.Comma, StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on missing column" in {
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        StringLiteral(""), Operators.Comma, StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on missing literals" in {
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        Operators.DefinedAs,
        NewLine)).isLeft shouldBe true
    }

    "fail on missing commas" in {
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        Operators.DefinedAs,
        StringLiteral(""), StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on malformed literal list" in {
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        Operators.DefinedAs,
        StringLiteral(""), Operators.Comma,
        NewLine)).isLeft shouldBe true
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        Operators.DefinedAs,
        Operators.Comma, StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on missing new line" in {
      run(HwmParser.whitelistParser, List(Keywords.Whitelist,
        Operators.DefinedAs,
        StringLiteral(""), Operators.Comma, StringLiteral(""))
      ).isLeft shouldBe true
    }
  }

  "blacklist parser" should {
    "parse expected format with one literal" in {
      val literal = "literal"
      run(HwmParser.blacklistParser, List(Keywords.Blacklist, Operators.DefinedAs, StringLiteral(literal), NewLine)) shouldBe Right(List(literal))
    }

    "parse expected format with multiple literals" in {
      val literal1 = "literal1"
      val literal2 = "literal2"
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        Operators.DefinedAs,
        StringLiteral(literal1), Operators.Comma, StringLiteral(literal2),
        NewLine)) shouldBe Right(List(literal1, literal2))
    }

    "fail on missing whitelist" in {
      run(HwmParser.blacklistParser, List(Operators.DefinedAs,
        StringLiteral(""), Operators.Comma, StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on missing column" in {
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        StringLiteral(""), Operators.Comma, StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on missing literals" in {
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        Operators.DefinedAs,
        NewLine)).isLeft shouldBe true
    }

    "fail on missing commas" in {
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        Operators.DefinedAs,
        StringLiteral(""), StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on malformed literal list" in {
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        Operators.DefinedAs,
        StringLiteral(""), Operators.Comma,
        NewLine)).isLeft shouldBe true
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        Operators.DefinedAs,
        Operators.Comma, StringLiteral(""),
        NewLine)).isLeft shouldBe true
    }

    "fail on missing new line" in {
      run(HwmParser.blacklistParser, List(Keywords.Blacklist,
        Operators.DefinedAs,
        StringLiteral(""), Operators.Comma, StringLiteral(""))
      ).isLeft shouldBe true
    }
  }

  "mode parser" should {
    "parse mode definition" in {
      val mode = "STRICT"
      run(HwmParser.modeParser, List(
        Keywords.Mode,
        Operators.DefinedAs,
        Identifier(mode),
        NewLine
      )) shouldBe Right(mode)
    }

    "fail on missing keyword" in {
      run(HwmParser.modeParser, List(
        Operators.DefinedAs,
        Identifier(""),
        NewLine
      )).isLeft shouldBe true
    }

    "fail on missing column" in {
      run(HwmParser.modeParser, List(
        Keywords.Mode,
        Identifier(""),
        NewLine
      )).isLeft shouldBe true
    }

    "fail on missing identifier" in {
      run(HwmParser.modeParser, List(
        Keywords.Mode,
        Operators.DefinedAs,
        NewLine
      )).isLeft shouldBe true
    }

    "fail on missing newline" in {
      run(HwmParser.modeParser, List(
        Keywords.Mode,
        Operators.DefinedAs,
        Identifier("")
      )).isLeft shouldBe true
    }
  }

  "module definition parser" should {
    "parse module definition with one literal" in {
      val identifier = "identifier"
      val literal = "literal"
      run(HwmParser.moduleDefinitionParser, List(Identifier(identifier),
        Operators.Equals,
        StringLiteral(literal), NewLine
      )) shouldBe Right(ModuleDefinition(identifier, List(literal)))
    }

    "parse module definition with multiple literals" in {
      val identifier = "identifier"
      val literal1 = "literal1"
      val literal2 = "literal2"
      run(HwmParser.moduleDefinitionParser, List(Identifier(identifier),
        Operators.Equals,
        StringLiteral(literal1), Operators.Comma, StringLiteral(literal2),
        NewLine
      )) shouldBe Right(ModuleDefinition(identifier, List(literal1, literal2)))
    }

    "fail on missing identifier" in {
      run(HwmParser.moduleDefinitionParser, List(
        Operators.Equals,
        StringLiteral(""), Operators.Comma, StringLiteral(""),
        NewLine
      )).isLeft shouldBe true
    }

    "fail on missing column" in {
      run(HwmParser.moduleDefinitionParser, List(Identifier(""),
        StringLiteral(""), Operators.Comma, StringLiteral(""),
        NewLine
      )).isLeft shouldBe true
    }

    "fail on missing literals" in {
      run(HwmParser.moduleDefinitionParser, List(Identifier(""),
        Operators.Equals,
        NewLine
      )).isLeft shouldBe true
    }

    "fail on malformed literals" in {
      run(HwmParser.moduleDefinitionParser, List(Identifier(""),
        Operators.Equals,
        Operators.Comma, StringLiteral(""),
        NewLine
      )).isLeft shouldBe true
      run(HwmParser.moduleDefinitionParser, List(Identifier(""),
        Operators.Equals,
        StringLiteral(""), Operators.Comma,
        NewLine
      )).isLeft shouldBe true
    }

    "fail on missing newline" in {
      run(HwmParser.moduleDefinitionParser, List(Identifier(""),
        Operators.Equals,
        StringLiteral(""), Operators.Comma, StringLiteral("")
      )).isLeft shouldBe true
    }
  }

  "chain dependency rules parser" should {
    "parse multiple chained rules" in {
      val id1 = "id1"
      val id2 = "id2"
      val id3 = "id3"
      run(HwmParser.chainDependencyRuleParser, List(Identifier(id1),
        Operators.Arrow,
        Identifier(id2), Operators.Arrow, Identifier(id3),
        NewLine
      )) shouldBe Right(ChainDependencyRule(List(id1, id2, id3)))
    }

    "parse one chained rule" in {
      val id1 = "id1"
      val id2 = "id2"
      run(HwmParser.chainDependencyRuleParser, List(Identifier(id1),
        Operators.Arrow,
        Identifier(id2),
        NewLine
      )) shouldBe Right(ChainDependencyRule(List(id1, id2)))
    }

    "fail on missing identifier" in {
      run(HwmParser.chainDependencyRuleParser, List(Operators.Arrow,
        Identifier(""),
        NewLine
      )).isLeft shouldBe true
      run(HwmParser.chainDependencyRuleParser, List(Identifier(""),
        Operators.Arrow,
        NewLine
      )).isLeft shouldBe true
      run(HwmParser.chainDependencyRuleParser, List(Identifier(""),
        NewLine
      )).isLeft shouldBe true
    }
  }

  "no dependent rule" should {
    "parse expected format" in {
      val id1 = "id1"
      val id2 = "id2"
      run(HwmParser.noDependencyRuleParser, List(
        Identifier(id1),
        Operators.NotArrow,
        Identifier(id2),
        NewLine
      )) shouldBe Right(NoDependentRule(id1, id2))
    }

    "fail on malformed identifiers" in {
      run(HwmParser.noDependencyRuleParser, List(
        Operators.NotArrow,
        Identifier(""),
        NewLine
      )).isLeft shouldBe true
      run(HwmParser.noDependencyRuleParser, List(
        Identifier(""),
        Identifier(""),
        NewLine
      )).isLeft shouldBe true
      run(HwmParser.noDependencyRuleParser, List(
        Identifier(""),
        Operators.NotArrow,
        NewLine
      )).isLeft shouldBe true
    }
  }

  "one to many rule" should {
    "parse rule with one identifier" in {
      val id1 = "id1"
      val id2 = "id2"
      run(HwmParser.oneToManyRuleParser, List(
        Identifier(id1),
        Operators.Arrow,
        Operators.OpenParenthesis,
        Identifier(id2),
        Operators.CloseParenthesis
      )) shouldBe Right(OneToManyRule(id1, List(id2)))
    }

    "parse rule with many identifiers" in {
      val id1 = "id1"
      val id2 = "id2"
      val id3 = "id3"
      run(HwmParser.oneToManyRuleParser, List(
        Identifier(id1),
        Operators.Arrow,
        Operators.OpenParenthesis,
        Identifier(id2),
        Operators.Comma,
        Identifier(id3),
        Operators.CloseParenthesis
      )) shouldBe Right(OneToManyRule(id1, List(id2, id3)))
    }

    "fail on missing identifier" in {
      run(HwmParser.oneToManyRuleParser, List(
        Operators.Arrow,
        Operators.OpenParenthesis,
        Identifier(""),
        Operators.CloseParenthesis
      )).isLeft shouldBe true

      run(HwmParser.oneToManyRuleParser, List(
        Identifier(""),
        Operators.Arrow,
        Operators.OpenParenthesis,
        Operators.CloseParenthesis
      )).isLeft shouldBe true
    }

    "fail on missing arrow" in {
      run(HwmParser.oneToManyRuleParser, List(
        Identifier(""),
        Operators.OpenParenthesis,
        Identifier(""),
        Operators.CloseParenthesis
      )).isLeft shouldBe true
    }

    "fail on missing open parenthesis" in {
      run(HwmParser.oneToManyRuleParser, List(
        Identifier(""),
        Operators.Arrow,
        Identifier(""),
        Operators.CloseParenthesis
      )).isLeft shouldBe true
    }

    "fail on missing close parenthesis" in {
      run(HwmParser.oneToManyRuleParser, List(
        Identifier(""),
        Operators.Arrow,
        Operators.OpenParenthesis,
        Identifier("")
      )).isLeft shouldBe true
    }
  }

  "many to one rule" should {
    "parse rule with one identifier" in {
      val id1 = "id1"
      val id2 = "id2"
      run(HwmParser.manyToOneRuleParser, List(
        Operators.OpenParenthesis,
        Identifier(id1),
        Operators.CloseParenthesis,
        Operators.Arrow,
        Identifier(id2)
      )) shouldBe Right(ManyToOneRule(List(id1), id2))
    }

    "parse rule with many identifiers" in {
      val id1 = "id1"
      val id2 = "id2"
      val id3 = "id3"
      run(HwmParser.manyToOneRuleParser, List(
        Operators.OpenParenthesis,
        Identifier(id1),
        Operators.Comma,
        Identifier(id2),
        Operators.CloseParenthesis,
        Operators.Arrow,
        Identifier(id3)
      )) shouldBe Right(ManyToOneRule(List(id1, id2), id3))
    }

    "fail on missing identifier" in {
      run(HwmParser.manyToOneRuleParser, List(
        Operators.OpenParenthesis,
        Operators.CloseParenthesis,
        Operators.Arrow,
        Identifier("")
      )).isLeft shouldBe true

      run(HwmParser.manyToOneRuleParser, List(
        Operators.OpenParenthesis,
        Identifier(""),
        Operators.CloseParenthesis,
        Operators.Arrow,
      )).isLeft shouldBe true
    }

    "fail on missing arrow" in {
      run(HwmParser.manyToOneRuleParser, List(
        Operators.OpenParenthesis,
        Identifier(""),
        Operators.CloseParenthesis,
        Identifier("")
      )).isLeft shouldBe true
    }

    "fail on missing open parenthesis" in {
      run(HwmParser.manyToOneRuleParser, List(
        Identifier(""),
        Operators.CloseParenthesis,
        Operators.Arrow,
        Identifier("")
      )).isLeft shouldBe true
    }

    "fail on missing close parenthesis" in {
      run(HwmParser.manyToOneRuleParser, List(
        Operators.OpenParenthesis,
        Identifier(""),
        Operators.Arrow,
        Identifier("")
      )).isLeft shouldBe true
    }
  }

  "parser" should {
    val completeDefinition = Definition(
      Some("com.pitest.highwheel."),
      Some(List("com.pitest.highwheel.", "foo")),
      Some(List("com.pitest.highwheel.", "bar")),
      Some("SOMETHING"),
      List(
        ModuleDefinition("Core", List("com.pitest.highwheel.core.*", "com.pitest.highwheel.core2.*")),
        ModuleDefinition("Utils", List("com.pitest.highwheel.utils.*")),
        ModuleDefinition("Modules", List("com.pitest.highwheel.modules.*")),
        ModuleDefinition("Parser", List("com.pitest.highwheel.parser.*"))
      ),
      List(
        ChainDependencyRule(List("Parser", "Core", "Utils")),
        NoDependentRule("Utils", "Core"),
        NoDependentRule("Utils", "Parser"),
        ChainDependencyRule(List("Modules", "Core")),
        ChainDependencyRule(List("Modules", "Utils")),
        OneToManyRule("Modules", List("Core", "Utils")),
        ManyToOneRule(List("Modules", "Core"), "Utils")
      )
    )

    val looseModules = List(
      ModuleDefinition("Main", List("org.example.Main")),
      ModuleDefinition("Controller", List("org.example.controller.*")),
      ModuleDefinition("Facade", List("org.example.core.CoreFacade")),
      ModuleDefinition("CoreInternals", List("org.example.core.internals.*")),
      ModuleDefinition("CoreApi", List("org.example.core.api.*")),
      ModuleDefinition("Model", List("org.example.core.model.*")),
      ModuleDefinition("IO", List("org.example.io.*")),
      ModuleDefinition("Utils", List("org.example.commons.*"))
    )
    val resourceExpectations = List(
      ("alternate-strict-spec.hwm", Definition(
        None, None, None, Some("STRICT"),
        List(
          ModuleDefinition("Internals", List("org.example.commons.*", "org.example.controller.*", "org.example.core.*", "org.example.io.*")),
          ModuleDefinition("Main", List("org.example.Main"))
        ),
        List(
          ChainDependencyRule(List("Main", "Internals"))
        )
      )),
      ("example-def.txt", completeDefinition.copy(prefix = None, whitelist = None, blacklist = None, mode = None)),
      ("example-def-with-blacklist.txt", completeDefinition.copy(prefix = None, whitelist = None, mode = None)),
      ("example-def-with-prefix.txt", completeDefinition.copy(whitelist = None, blacklist = None, mode = None)),
      ("example-def-with-prefix-wl-bl.txt", completeDefinition.copy(mode = None)),
      ("example-def-with-prefix-wl-bl-mode.txt", completeDefinition),
      ("loose-spec.hwm", Definition(
        None, None, None, Some("LOOSE"),
        looseModules,
        List(
          ChainDependencyRule(List("Main", "Controller", "Facade")),
          ChainDependencyRule(List("Main", "Model")),
          ChainDependencyRule(List("Main", "IO")),
          ChainDependencyRule(List("Facade", "CoreInternals", "Model")),
          ChainDependencyRule(List("CoreApi", "Model")),
          ChainDependencyRule(List("IO", "CoreApi")),
          ChainDependencyRule(List("IO", "Model")),
          NoDependentRule("IO", "CoreInternals"),
          NoDependentRule("Utils", "Main")
        )
      )),
      ("loose-spec-whiteblack.hwm", Definition(
        None,
        Some(List("org.example.*")),
        Some(List("org.example.Main", "org.example.commons.*")),
        Some("LOOSE"),
        looseModules, List(
          ChainDependencyRule(List("Controller", "Facade")),
          ChainDependencyRule(List("Facade", "CoreInternals", "Model")),
          ChainDependencyRule(List("CoreApi", "Model")),
          ChainDependencyRule(List("IO", "CoreApi")),
          ChainDependencyRule(List("IO", "Model")),
          NoDependentRule("IO", "CoreInternals"),
          NoDependentRule("Utils", "Main")
        )
      )),
      ("spec.hwm", Definition(
        Some("org.example."), None, None, None,
        List(
          ModuleDefinition("Main", List("Main")),
          ModuleDefinition("Controller", List("controller.*")),
          ModuleDefinition("Facade", List("core.CoreFacade")),
          ModuleDefinition("CoreInternals", List("core.internals.*")),
          ModuleDefinition("CoreApi", List("core.api.*", "core.otherapi.*")),
          ModuleDefinition("Model", List("core.model.*")),
          ModuleDefinition("IO", List("io.*")),
          ModuleDefinition("Utils", List("commons.*"))
        ),
        List(
          ChainDependencyRule(List("Main", "Controller", "Facade")),
          NoDependentRule("Main", "CoreInternals"),
          NoDependentRule("Controller", "CoreInternals"),
          OneToManyRule("Main", List("Facade", "CoreApi", "IO")),
          ChainDependencyRule(List("Controller", "Facade")),
          ChainDependencyRule(List("CoreInternals", "Model")),
          ChainDependencyRule(List("CoreInternals", "Utils")),
          ChainDependencyRule(List("Facade", "CoreInternals", "CoreApi")),
          ChainDependencyRule(List("Facade", "CoreApi")),
          ManyToOneRule(List("Facade", "CoreApi"), "Model"),
          ChainDependencyRule(List("IO", "CoreApi")),
          ChainDependencyRule(List("IO", "Model")),
          ChainDependencyRule(List("IO", "Utils")),
          NoDependentRule("IO", "CoreInternals")
        )
      ))
    )

    resourceExpectations foreach {
      case (resource, expected) => s"parse $resource" in {
        compileResource(resource) shouldBe Right(expected)
      }
    }
  }

  def compile(s: String): Either[ParserException, Definition] = {
    val tokens = lex(s).toOption.get
    HwmParser.parse(tokens)
  }

  def compileResource(resource: String): Either[ParserException, Definition] = {
    val tokens = lexResource(resource).toOption.get
    HwmParser.parse(tokens)
  }

  def lexResource(resource: String): Either[Exception, List[HwmToken]] = {
    val reader = StreamReader(Source.fromResource(resource).reader())
    HwmLexer(reader)
  }

  def lex(s: String): Either[ParserException, List[HwmToken]] = HwmLexer(new CharArrayReader(s.toCharArray))

  def run[T](p: HwmParser.Parser[T], input: List[HwmToken]): Either[String, T] =
    HwmParser.interpret(p, toInput(input))

  def toInput(l: List[HwmToken]): HwmParser.Input = new Reader[HwmToken] {
    override def first: HwmToken = l.head

    override def rest: Reader[HwmToken] = toInput(l.tail)

    override def pos: Position = NoPosition

    override def atEnd: Boolean = l.isEmpty
  }
}
