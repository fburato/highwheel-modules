package com.github.fburato.highwheelmodules.utils

import org.apache.commons.lang3.RandomUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.ByteArrayInputStream

class StreamUtilTest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private def byteArray: Array[Byte] = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0xa)

  "should copy streams to byte arrays" in {
    val expected = byteArray
    val bis = new ByteArrayInputStream(expected)
    val actual = StreamUtilS.streamToByteArray(bis)

    actual shouldEqual expected
  }

  "should copy streams larger than buffer size" in {
    val expected = RandomUtils.nextBytes(10 * 1024 + 17)
    val bis = new ByteArrayInputStream(expected)
    val actual = StreamUtilS.streamToByteArray(bis)

    actual shouldEqual expected
  }

  "should copy contents of stream to other" in {
    val expected = RandomUtils.nextBytes(10 * 1024 + 17)
    val actualStream =
      StreamUtilS.copyStream(new ByteArrayInputStream(Array.copyAs(expected, expected.length)))
    val actualContents = StreamUtilS.streamToByteArray(actualStream)

    actualContents shouldEqual expected
  }
}
