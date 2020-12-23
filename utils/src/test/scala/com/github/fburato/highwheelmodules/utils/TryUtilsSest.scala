package com.github.fburato.highwheelmodules.utils

import com.github.fburato.highwheelmodules.utils.TryUtils._
import org.apache.commons.lang3.RandomUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success}

class TryUtilsSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  "sequence of empty should be Success of empty" in {
    sequence(Seq()) shouldEqual Success(Seq())
  }

  "sequence of successes should be success of sequence" in {
    val bytes: Seq[Byte] = RandomUtils.nextBytes(23).toSeq
    val successBytes = bytes.map(Success(_))

    sequence(successBytes) shouldEqual Success(bytes)
  }

  "failure in the sequence should cause the entire sequence to fail" in {
    val failure = new RuntimeException

    sequence(Seq(Failure(failure), Success(2), Success(3))) shouldEqual Failure(failure)
    sequence(Seq(Success(1), Failure(failure), Success(3))) shouldEqual Failure(failure)
    sequence(Seq(Success(1), Success(2), Failure(failure))) shouldEqual Failure(failure)
  }
}
