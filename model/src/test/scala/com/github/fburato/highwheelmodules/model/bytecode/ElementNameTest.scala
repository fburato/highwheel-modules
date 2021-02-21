package com.github.fburato.highwheelmodules.model.bytecode

import org.apache.commons.lang3.RandomStringUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ElementNameTest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private def random: String = RandomStringUtils.randomAlphanumeric(20)

  "fromString internal representation of alphanumeric string should be identical to string itself" in {
    val ran = random

    ElementName.fromString(ran).asInternalName shouldEqual ran
  }

  "fromString internal representation of string with '.' should be equal to string with '.' replaced for '/'" in {
    val (ran1, ran2, ran3) = (random, random, random)

    ElementName.fromString(s"$ran1.$ran2.$ran3").asInternalName shouldEqual s"$ran1/$ran2/$ran3"
  }

  "fromClass internal representation should be the fully qualified string name with '.' converted to '/'" in {
    ElementName.fromClass(classOf[String]).asInternalName shouldEqual classOf[String].getName
      .replace('.', '/')
  }

  "elements that resolve to same internal name should be equal" in {
    val (ran1, ran2, ran3) = (random, random, random)

    ElementName.fromString(s"$ran1.$ran2.$ran3") shouldEqual ElementName.fromString(
      s"$ran1/$ran2/$ran3"
    )
  }

  "javaName of ElementName should be equal to internal representation with '/' converted to '.'" in {
    val stringElement = ElementName.fromClass(classOf[String])

    stringElement.asJavaName shouldEqual classOf[String].getName
    stringElement.asJavaName shouldEqual stringElement.asInternalName.replace('/', '.')
  }

  "ordering should should be based on internal representation" in {
    import ElementName.ordering

    val names = (1 until 10).map(_ => random)
    val elementNames = names.map(ElementName.fromString)

    val zipped = names.sorted zip elementNames.sorted
    zipped.foreach { case (name, elementName) =>
      elementName.asInternalName shouldEqual name
    }
  }
}
