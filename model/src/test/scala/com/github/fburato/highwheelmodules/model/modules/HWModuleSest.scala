package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import org.apache.commons.lang3.RandomStringUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HWModuleSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  "make" should {
    val name = RandomStringUtils.randomAlphanumeric(20)
    "be empty if regex passed is invalid" in {
      HWModuleS.make(name, List("[asdf")) shouldBe None
    }

    "be empty if any regex passed is invalid" in {
      HWModuleS.make(name, List("valid", "[asdf")) shouldBe None
    }

    "be defined if regex passed is valid" in {
      HWModuleS.make(name, List("org.example.*")).isDefined shouldBe true
    }

    "be defined if all regexes passed are valid" in {
      HWModuleS.make(name, List("org.example.*", "valid")).isDefined shouldBe true
    }
  }

  "contains" should {
    val globPattern = List("org.example.foo*", "org.example.bar*")
    val name = RandomStringUtils.randomAlphanumeric(20)
    val testee = HWModuleS.make(name, globPattern).get

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
    val name = RandomStringUtils.randomAlphanumeric(20)

    HWModuleS.make(name, List("a")).get shouldEqual HWModuleS.make(name, List("a")).get
    HWModuleS.make(name, List("a", "b")).get shouldEqual HWModuleS.make(name, List("b", "a")).get
    HWModuleS.make(name, List("a")).get should not equal HWModuleS.make(name, List("a", "b")).get
    HWModuleS.make(name, List("a", "b")).get should not equal HWModuleS.make(name, List("a")).get
  }

  "equals should distinguish between modules with different names" in {
    val name = RandomStringUtils.randomAlphanumeric(20)

    HWModuleS.make(name, List("a")).get should not equal HWModuleS.make("NOT" + name, List("a")).get
  }
}
