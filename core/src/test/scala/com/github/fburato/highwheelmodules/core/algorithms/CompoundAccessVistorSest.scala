package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, ElementNameS, USES}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.mockito.Mockito
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CompoundAccessVistorSest extends AnyWordSpec with Matchers with MockitoSugar {

  private val av1 = mock[AccessVisitor]
  private val av2 = mock[AccessVisitor]
  private val testee = CompoundAccessVisitor(Seq(av1, av2))
  private val order = Mockito.inOrder(av1, av2)
  private val ap = AccessPointS(ElementNameS.fromString("a"))
  private val at = USES
  private val en = ElementNameS.fromString("b")

  "apply should call delegated apply in order" in {
    testee(ap, ap, at)

    order.verify(av1).apply(ap, ap, at)
    order.verify(av2).apply(ap, ap, at)
  }

  "newAccessPoint should call delegated newAccessPoint in order" in {
    testee.newAccessPoint(ap)

    order.verify(av1).newAccessPoint(ap)
    order.verify(av2).newAccessPoint(ap)
  }

  "newNode should call delegated newNode in order" in {
    testee.newNode(en)

    order.verify(av1).newNode(en)
    order.verify(av2).newNode(en)
  }

  "newEntryPoint should call delegate newEntryPoint in order" in {
    testee.newEntryPoint(en)

    order.verify(av1).newEntryPoint(en)
    order.verify(av2).newEntryPoint(en)
  }
}
