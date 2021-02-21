package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.bytecodeparser.TryMatchers._
import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{File, InputStream}
import java.nio.file.Paths
import scala.util.Success

class DirectoryClassPathRootTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private val invalidRootTestee: DirectoryClassPathRoot =
    new DirectoryClassPathRoot(new File("foo"))

  private val validRootTestee =
    new DirectoryClassPathRoot(Paths.get("parser", "target", "scala-2.13", "test-classes").toFile)

  "getData" should {
    "return an input stream for an existing class" in {

      validRootTestee.getData(
        ElementName.fromClass(classOf[DirectoryClassPathRootTest])
      ) should beSuccessWith[InputStream] { stream =>
        stream should not be null
      }
    }

    "return null for unrecognised class" in {

      validRootTestee.getData(ElementName.fromString("Foo")) shouldEqual Success(null)
    }

    "return null for not existing class" in {

      invalidRootTestee.getData(ElementName.fromString("Foo")) shouldEqual Success(null)
    }
  }

  "classNames" should {
    "return empty collection for invalid root" in {

      invalidRootTestee.classNames shouldEqual Success(Seq())
    }

    "return collection containing the expected elements" in {

      validRootTestee.classNames should beSuccessWith[Seq[ElementName]] { names =>
        names should contain(ElementName.fromClass(classOf[DirectoryClassPathRootTest]))
      }
    }
  }
}
