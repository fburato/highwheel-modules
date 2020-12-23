package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.bytecodeparser.TryMatchers._
import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{File, InputStream}
import java.nio.file.Paths
import scala.util.Success

class DirectoryClassPathRootSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val invalidRootTestee: DirectoryClassPathRoot =
    new DirectoryClassPathRoot(new File("foo"))

  private val validRootTestee =
    new DirectoryClassPathRoot(Paths.get("target", "test-classes").toFile)


  "getData" should {
    "return an input stream for an existing class" in {

      validRootTestee.getData(ElementNameS.fromClass(classOf[DirectoryClassPathRootSest])) should beSuccessWith[InputStream] { stream =>
        stream should not be null
      }
    }

    "return null for unrecognised class" in {

      validRootTestee.getData(ElementNameS.fromString("Foo")) shouldEqual Success(null)
    }

    "return null for not existing class" in {

      invalidRootTestee.getData(ElementNameS.fromString("Foo")) shouldEqual Success(null)
    }
  }

  "classNames" should {
    "return empty collection for invalid root" in {

      invalidRootTestee.classNames shouldEqual Success(Seq())
    }

    "return collection containing the expected elements" in {

      validRootTestee.classNames should beSuccessWith[Seq[ElementNameS]] { names =>
        names should contain(ElementNameS.fromClass(classOf[DirectoryClassPathRootSest]))
      }
    }
  }
}
