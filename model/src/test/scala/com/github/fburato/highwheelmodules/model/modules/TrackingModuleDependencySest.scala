package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, ElementNameS}
import org.apache.commons.lang3.RandomStringUtils
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TrackingModuleDependencySest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private def randomString: String = RandomStringUtils.randomAlphanumeric(10)

  private val moduleA = HWModuleS.make(randomString, List(randomString)).get
  private val moduleB = HWModuleS.make(randomString, List(randomString)).get
  private val exampleSource1 = AccessPointS(ElementNameS.fromString(randomString))
  private val exampleSource2 = AccessPointS(ElementNameS.fromString(randomString))
  private val exampleDest1 = AccessPointS(ElementNameS.fromString(randomString))
  private val exampleDest2 = AccessPointS(ElementNameS.fromString(randomString))
  private val testee = TrackingModuleDependencyS(moduleA, moduleB)

  "evidences should be empty on empty tracking dependency" in {
    testee.sources shouldEqual Set()
    testee.destinations shouldEqual Set()
  }

  "sources should return added evidence" in {
    testee.addEvidence(exampleSource1, exampleDest1)

    testee.sources should contain theSameElementsAs Set(exampleSource1)
    testee.destinations should contain theSameElementsAs Set(exampleDest1)
  }

  "getDestinationFromSources" should {
    "be empty on no evidence present" in {
      testee.destinationsFromSource(exampleSource1) shouldEqual Set()
    }

    "be empty evidence if no evidence added with requested source" in {
      testee.addEvidence(exampleSource2, exampleDest1)

      testee.destinationsFromSource(exampleSource1) shouldEqual Set()
    }

    "return the expected evidence" in {
      testee.addEvidence(exampleSource1, exampleDest1)

      testee.destinationsFromSource(exampleSource1) should contain theSameElementsAs Set(exampleDest1)
    }
  }

  "addEvidence" should {
    val testee = TrackingModuleDependencyS(moduleA, moduleB)

    "save all evidence from same source" in {
      testee.addEvidence(exampleSource1, exampleDest1)
      testee.addEvidence(exampleSource1, exampleDest2)

      testee.sources should contain theSameElementsAs Set(exampleSource1)
      testee.destinations should contain theSameElementsAs Set(exampleDest1, exampleDest2)
      testee.evidenceCounter shouldEqual 2
    }

    "save all evidence from different sources" in {
      testee.addEvidence(exampleSource1, exampleDest1)
      testee.addEvidence(exampleSource1, exampleDest2)
      testee.addEvidence(exampleSource2, exampleDest1)
      testee.addEvidence(exampleSource2, exampleDest2)

      testee.sources should contain theSameElementsAs Set(exampleSource1, exampleSource2)
      testee.destinations should contain theSameElementsAs Set(exampleDest1, exampleDest2)
      testee.evidenceCounter shouldEqual 4
    }
  }
}
