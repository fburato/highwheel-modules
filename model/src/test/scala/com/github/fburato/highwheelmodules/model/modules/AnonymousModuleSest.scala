package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AnonymousModuleSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  "make" should {
    "be empty if regex passed is invalid" in {
      AnonymousModuleS.make(List("[asdf")) shouldBe None
    }

    "be empty if any regex passed is invalid" in {
      AnonymousModuleS.make(List("valid", "[asdf")) shouldBe None
    }

    "be defined if regex passed is valid" in {
      AnonymousModuleS.make(List("org.example.*")).isDefined shouldBe true
    }

    "be defined if all regexes passed are valid" in {
      AnonymousModuleS.make(List("org.example.*", "valid")).isDefined shouldBe true
    }
  }

  "contains" should {
    val globPattern = List("org.example.foo*", "org.example.bar*")
    val testee = AnonymousModuleS.make(globPattern).get

    "match if name matches any pattern" in {
      testee.contains(ElementNameS.fromString("org.example.foo.Something")) shouldBe true
      testee.contains(ElementNameS.fromString("org.example.bar.Something")) shouldBe true
    }

    "match with globs rather than regex" in {
      testee.contains(ElementNameS.fromString("org|example|foo|Something")) shouldBe false
    }

    "use all glob allowed characters" in {
      def makeTestee(glob: String): AnonymousModuleS =
        AnonymousModuleS.make(List(glob)).get

      def name(s: String): ElementNameS = ElementNameS.fromString(s)

      makeTestee("$a$").contains(name("$a$")) shouldBe true
      makeTestee("^a$").contains(name("a")) shouldBe false
      makeTestee("a?").contains(name("ab")) shouldBe true
      makeTestee("a?").contains(name("b")) shouldBe false
      makeTestee("a.").contains(name("ab")) shouldBe false
      makeTestee("a.").contains(name("a.")) shouldBe true
      makeTestee("a\\").contains(name("a\\")) shouldBe true
    }

    "not match if name doesn't match any pattern" in {
      testee.contains(ElementNameS.fromString("notorg.example.foo")) shouldBe false
      testee.contains(ElementNameS.fromString("org.example.zoo")) shouldBe false
      testee.contains(ElementNameS.fromString("org.zxample.foo")) shouldBe false
    }
  }

  "equals should work on pattern literals irrespective of order" in {
    AnonymousModuleS.make(List("a")).get shouldEqual AnonymousModuleS.make(List("a")).get
    AnonymousModuleS.make(List("a", "b")).get shouldEqual AnonymousModuleS.make(List("b", "a")).get
    AnonymousModuleS.make(List("a")).get should not equal AnonymousModuleS.make(List("a", "b")).get
    AnonymousModuleS.make(List("a", "b")).get should not equal AnonymousModuleS.make(List("a")).get
  }
}
