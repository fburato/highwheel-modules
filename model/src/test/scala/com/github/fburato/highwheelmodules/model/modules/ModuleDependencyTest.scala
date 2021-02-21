package com.github.fburato.highwheelmodules.model.modules

import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ModuleDependencyTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private val moduleA = HWModule.make("a", List("a")).get
  private val moduleB = HWModule.make("b", List("b")).get

  "count should be 0 on new dependency" in {
    ModuleDependency(moduleA, moduleB).count shouldBe 0
  }

  "count should increase on incrementCount invocation" in {
    val testee = ModuleDependency(moduleA, moduleB)

    testee.incrementCount()

    testee.count shouldBe 1
  }

  "equals should compare all elements" in {
    val testee = ModuleDependency(moduleA, moduleB)
    val alternateTestee = ModuleDependency(moduleA, HWModule.make(moduleB.name, List("b")).get)

    testee shouldEqual alternateTestee

    testee.incrementCount()

    testee should not equal alternateTestee
  }
}
