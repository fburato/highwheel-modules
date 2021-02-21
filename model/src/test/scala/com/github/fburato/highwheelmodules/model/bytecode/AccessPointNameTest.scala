package com.github.fburato.highwheelmodules.model.bytecode

import org.apache.commons.lang3.RandomStringUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AccessPointNameTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private def random: String = RandomStringUtils.randomAlphanumeric(20)

  "create" should {
    "replace all '<' for '(' and all '>' for ')' for name" in {
      val (ran1, ran2) = (random, random)
      val testee = AccessPointName.create(s"<$ran1<><<>", ran2)

      testee shouldEqual AccessPointName.create(s"($ran1()(()", ran2)
      testee.name shouldEqual s"($ran1()(()"
      testee.descriptor shouldEqual ran2
    }
  }
}
