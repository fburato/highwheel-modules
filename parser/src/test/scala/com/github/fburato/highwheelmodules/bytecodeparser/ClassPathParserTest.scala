package com.github.fburato.highwheelmodules.bytecodeparser

import com.github.fburato.highwheelmodules.bytecodeparser.TryMatchers._
import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.{AccessVisitor, ClasspathRoot}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{IOException, InputStream}
import scala.util.Success

class ClassPathParserTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private val classPathRoot = mock[ClasspathRoot]
  private val fooString = "foo"
  private val testee = new ClassPathParser(element => element.asInternalName == fooString)

  "parse" should {
    "not parse classes that do not match filter" in {
      val bar = ElementName.fromString("bar")
      when(classPathRoot.classNames).thenReturn(Success(Seq(bar)))

      testee.parse(classPathRoot, mock[AccessVisitor]).get

      verify(classPathRoot, never).getData(any)
    }

    "close class input streams even if they fail" in {
      val foo = ElementName.fromString(fooString)
      when(classPathRoot.classNames).thenReturn(Success(Seq(foo)))
      val is = mock[InputStream]
      when(is.available()).thenReturn(1)
      when(classPathRoot.getData(foo)).thenReturn(Success(is))
      val exception = new IOException()
      doThrow(exception).when(is).read(any, anyInt, anyInt)

      testee.parse(classPathRoot, mock[AccessVisitor]) should beFailureWith[Unit] { exception =>
        exception shouldBe theSameInstanceAs(exception)
      }

      verify(is).close()
    }
  }
}
