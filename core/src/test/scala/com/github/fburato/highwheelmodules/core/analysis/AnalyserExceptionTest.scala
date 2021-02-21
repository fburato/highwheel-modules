package com.github.fburato.highwheelmodules.core.analysis

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Success, Try}

class AnalyserExceptionTest extends AnyWordSpec with Matchers {
  "construction" should {
    "not fail on null msg" in {
      Try(AnalyserException(null, new RuntimeException)) should matchPattern { case Success(_) =>
      }
    }

    "not fail on null cause" in {
      Try(AnalyserException("foobar", null)) should matchPattern { case Success(_) =>
      }
    }

    "not fail on null cause and message" in {
      Try(AnalyserException(null, null)) should matchPattern { case Success(_) =>
      }
    }
  }
}
