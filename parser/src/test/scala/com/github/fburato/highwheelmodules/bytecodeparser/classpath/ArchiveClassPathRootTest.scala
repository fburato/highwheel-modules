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

class ArchiveClassPathRootTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private val testee = new ArchiveClassPathRoot(Paths.get("parser", "mytests.jar").toFile)

  "classNames" should {
    "return all classes in archive" in {
      testee.classNames should beSuccessWith[Seq[ElementName]] { classNames =>
        classNames should contain theSameElementsAs Seq(
          ElementName.fromString("injar.p1.P1NoTest$InnerTest"),
          ElementName.fromString("injar.p1.P1NoTest"),
          ElementName.fromString("injar.p1.P1Test"),
          ElementName.fromString("injar.p2.P2Test")
        )
      }
    }
  }

  "getData" should {
    "return empty for unknown class" in {
      testee.getData(ElementName.fromString("bar")) shouldEqual Success(None)
    }

    "return input stream for known class" in {
      testee.getData(ElementName.fromString("injar.p1.P1Test")) should beSuccessWith[Option[InputStream]](
        result => result should not be empty
      )
    }

    "return a readable input stream" in {
      val bytes = new Array[Byte](100)

      testee
        .getData(ElementName.fromString("injar.p1.P1Test"))
        .map(maybeStream => maybeStream.get.read(bytes)) should beSuccess
    }
  }
}
