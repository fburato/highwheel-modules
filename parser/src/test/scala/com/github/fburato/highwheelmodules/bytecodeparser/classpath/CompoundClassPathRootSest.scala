package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.bytecodeparser.TryMatchers._
import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot
import org.apache.commons.lang3.RandomStringUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.ByteArrayInputStream
import scala.util.{Failure, Success}

class CompoundClassPathRootSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  private val child1 = mock[ClasspathRoot]
  private val child2 = mock[ClasspathRoot]
  private val testee = new ClassPathRoot(Seq(child1, child2))
  private val inputStream1 = new ByteArrayInputStream(new Array[Byte](10))
  private val inputStream2 = new ByteArrayInputStream(new Array[Byte](20))

  "getData" should {
    val elementName = ElementNameS.fromString(RandomStringUtils.randomAlphanumeric(20))
    "fail if any of the children fail" in {
      val exception = new RuntimeException
      when(child2.getData(any)) thenReturn Failure(exception)
      when(child1.getData(any)) thenReturn Success(inputStream1)

      testee.getData(elementName) shouldEqual Failure(exception)
    }

    "invoke getData on the children" in {
      when(child1.getData(any)) thenReturn Success(inputStream1)
      when(child2.getData(any)) thenReturn Success(inputStream1)

      testee.getData(elementName)

      verify(child1).getData(elementName)
      verify(child2).getData(elementName)
    }

    "return null if no child returns not null input stream" in {
      when(child1.getData(any)) thenReturn Success(null)
      when(child2.getData(any)) thenReturn Success(null)

      testee.getData(elementName) shouldEqual Success(null)
    }

    "return first not null child stream if every child returns" in {
      when(child1.getData(any)) thenReturn Success(inputStream1)
      when(child2.getData(any)) thenReturn Success(inputStream2)

      testee.getData(elementName) shouldEqual Success(inputStream1)
    }

    "return first not null child stream if some child returns null" in {
      when(child1.getData(any)) thenReturn Success(null)
      when(child2.getData(any)) thenReturn Success(inputStream2)

      testee.getData(elementName) shouldEqual Success(inputStream2)
    }
  }

  "getResource" should {
    val resourceName = RandomStringUtils.randomAlphanumeric(20)
    "fail if any of the children fail" in {
      val exception = new RuntimeException
      when(child2.getResource(any)) thenReturn Failure(exception)
      when(child1.getResource(any)) thenReturn Success(inputStream1)

      testee.getResource(resourceName) shouldEqual Failure(exception)
    }

    "invoke getData on the children" in {
      when(child1.getResource(any)) thenReturn Success(inputStream1)
      when(child2.getResource(any)) thenReturn Success(inputStream1)

      testee.getResource(resourceName)

      verify(child1).getResource(resourceName)
      verify(child2).getResource(resourceName)
    }

    "return null if no child returns not null input stream" in {
      when(child1.getResource(any)) thenReturn Success(null)
      when(child2.getResource(any)) thenReturn Success(null)

      testee.getResource(resourceName) shouldEqual Success(null)
    }

    "return first not null child stream if every child returns" in {
      when(child1.getResource(any)) thenReturn Success(inputStream1)
      when(child2.getResource(any)) thenReturn Success(inputStream2)

      testee.getResource(resourceName) shouldEqual Success(inputStream1)
    }

    "return first not null child stream if some child returns null" in {
      when(child1.getResource(any)) thenReturn Success(null)
      when(child2.getResource(any)) thenReturn Success(inputStream2)

      testee.getResource(resourceName) shouldEqual Success(inputStream2)
    }
  }

  "classNames" should {
    def generateRandomElementNames(size: Int): Seq[ElementNameS] =
      (0 until size) map (i => ElementNameS.fromString(RandomStringUtils.randomAlphanumeric(10 + i)))

    val child1ClassNames = generateRandomElementNames(5)
    val child2ClassNames = generateRandomElementNames(7)

    "fail if any of the children fails" in {
      val exception = new RuntimeException
      when(child2.classNames) thenReturn Failure(exception)
      when(child1.classNames) thenReturn Success(child1ClassNames)

      testee.classNames shouldEqual Failure(exception)
    }

    "invoke classNames in all children" in {
      when(child1.classNames) thenReturn Success(child1ClassNames)
      when(child2.classNames) thenReturn Success(child2ClassNames)

      testee.classNames

      verify(child1).classNames
      verify(child2).classNames
    }

    "returns a collection with all classNames returned by the children" in {
      when(child1.classNames) thenReturn Success(child1ClassNames)
      when(child2.classNames) thenReturn Success(child2ClassNames)

      testee.classNames should beSuccessWith[Seq[ElementNameS]] { results =>
        results should contain theSameElementsInOrderAs (child1ClassNames ++ child2ClassNames)
      }
    }

    "not fail if any of the children returns empty collection" in {
      when(child1.classNames) thenReturn Success(Seq())
      when(child2.classNames) thenReturn Success(child2ClassNames)

      testee.classNames should beSuccessWith[Seq[ElementNameS]] { results =>
        results should contain theSameElementsInOrderAs child2ClassNames
      }
    }

    "return empty collection if all children return empty collection" in {
      when(child1.classNames) thenReturn Success(Seq())
      when(child2.classNames) thenReturn Success(Seq())

      testee.classNames should beSuccessWith[Seq[ElementNameS]] { results =>
        results should contain theSameElementsInOrderAs Seq()
      }
    }
  }
}
