package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.bytecodeparser.TryMatchers._
import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.InputStream
import scala.util.Success

class ClassLoaderClassPathRootTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private val testee = new ClassLoaderClassPathRoot(this.getClass.getClassLoader)

  "classNames should return empty" in {
    testee.classNames shouldEqual Success(Seq())
  }

  "getData" should {
    "return not null for classes visible to loader" in {
      testee.getData(
        ElementName.fromClass(classOf[ClassLoaderClassPathRootTest])
      ) should beSuccessWith[Option[InputStream]](maybeStream => maybeStream should not be empty)
    }

    "return null for classes not visible to loader" in {
      testee.getData(ElementName.fromString("FooFoo")) shouldBe Success(None)
    }
  }

  "getResource" should {
    "return not null for resources visible to loader" in {
      testee.getResource("aresource.txt") should beSuccessWith[Option[InputStream]](maybeStream =>
        maybeStream should not be empty
      )
    }

    "return null for resource not visible to loader" in {
      testee.getResource("not defined") shouldBe Success(None)
    }
  }
}
