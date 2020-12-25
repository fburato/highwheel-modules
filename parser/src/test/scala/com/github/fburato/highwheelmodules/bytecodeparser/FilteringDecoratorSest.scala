package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessPointName, COMPOSED, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import org.apache.commons.lang3.RandomStringUtils
import org.mockito.ArgumentCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters.CollectionHasAsScala

class FilteringDecoratorSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val matchingString = RandomStringUtils.randomAlphanumeric(20)
  private val notMatchingString = "NOT" + matchingString
  private val matchingElement = ElementName.fromString(matchingString)
  private val notMatchingElement = ElementName.fromString(notMatchingString)
  private val matchingAccessPoint = AccessPoint(matchingElement, AccessPointName(matchingString, "()V"))
  private val notMatchingAccessPoint = AccessPoint(notMatchingElement, AccessPointName(notMatchingString, "()V"))

  private val delegate = mock[AccessVisitor]
  private val testee = new FilteringDecorator(delegate, el => el.asInternalName == matchingString)


  "FilteringDecorator" should {
    "not forward apply call when filter does not match source" in {
      testee(notMatchingAccessPoint, matchingAccessPoint, COMPOSED)

      verify(delegate, never).apply(any, any, any)
    }

    "not forward apply call when filter does not match dest" in {
      testee(matchingAccessPoint, notMatchingAccessPoint, COMPOSED)

      verify(delegate, never).apply(any, any, any)
    }

    "forward call apply when filter matches source and dest" in {
      testee(matchingAccessPoint, matchingAccessPoint, COMPOSED)

      verify(delegate).apply(matchingAccessPoint, matchingAccessPoint, COMPOSED)
    }

    "not forward call newNode when filter does not match elementName" in {
      testee.newNode(notMatchingElement)

      verify(delegate, never).newNode(any)
    }

    "forward call newNode when filter match elementName" in {
      testee.newNode(matchingElement)

      verify(delegate).newNode(matchingElement)
    }

    "not forward call newEntryPoint when filter does not match elementName" in {
      testee.newEntryPoint(notMatchingElement)

      verify(delegate, never).newEntryPoint(any)
    }

    "forward call newEntryPoint when filter does match elementName" in {
      testee.newEntryPoint(matchingElement)

      verify(delegate).newEntryPoint(matchingElement)
    }

    "forward newAccessPoint call regardless of the filter matching" in {
      testee.newAccessPoint(matchingAccessPoint)
      testee.newAccessPoint(notMatchingAccessPoint)

      val captor: ArgumentCaptor[AccessPoint] = ArgumentCaptor.forClass(classOf[AccessPoint])
      verify(delegate, times(2)).newAccessPoint(captor.capture)

      captor.getAllValues.asScala.toList should contain theSameElementsInOrderAs List(matchingAccessPoint, notMatchingAccessPoint)
    }
  }

}
