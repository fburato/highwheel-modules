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
    new DirectoryClassPathRoot(getAppropriateDirectory)

  private def getAppropriateDirectory: File = {
    val alternatives = Seq("scala-2.12", "scala-2.13")
    alternatives
      .map(scalaVersion => Paths.get("parser", "target", scalaVersion, "test-classes").toFile)
      .find(file => file.exists())
      .get
  }

  "getData" should {
    "return an input stream for an existing class" in {

      validRootTestee.getData(
        ElementName.fromClass(classOf[DirectoryClassPathRootTest])
      ) should beSuccessWith[Option[InputStream]] { maybeStream =>
        maybeStream should not be empty
      }
    }

    "return empty for unrecognised class" in {

      validRootTestee.getData(ElementName.fromString("Foo")) shouldEqual Success(None)
    }

    "return empty for not existing class" in {

      invalidRootTestee.getData(ElementName.fromString("Foo")) shouldEqual Success(None)
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
