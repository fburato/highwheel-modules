package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, ElementName}
import com.github.fburato.highwheelmodules.model.modules.{HWModule, TrackingModuleDependency}
import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class GuavaTrackingModuleGraphSest extends AnyWordSpec with Matchers with OneInstancePerTest {
  private val graph: MutableNetwork[HWModule, TrackingModuleDependency] = NetworkBuilder.directed()
    .allowsSelfLoops(true).build()
  private val testee = new GuavaTrackingModuleGraph(graph)

  def module(name: String, regex: String): HWModule = HWModule.make(name, Seq(regex)).get

  private val m1 = module("module a", "A")
  private val m2 = module("module b", "B")
  private val m3 = module("module c", "C")
  private val ap1 = AccessPoint(ElementName.fromString("ap1"))
  private val ap2 = AccessPoint(ElementName.fromString("ap2"))
  private val ap3 = AccessPoint(ElementName.fromString("ap3"))

  "addModule" should {
    "add node to network" in {
      testee.addModule(m1)

      graph.nodes().asScala should contain theSameElementsAs List(m1)
    }

    "not add a module multiple times" in {
      testee.addModule(m1)
      testee.addModule(m1)

      graph.nodes().asScala should contain theSameElementsAs List(m1)
    }
  }

  "addDependency" should {
    "create new edge if modules did not exist already" in {
      testee.addModule(m1)
      testee.addModule(m2)
      val dep = TrackingModuleDependency(m1, m2)
      dep.addEvidence(ap1, ap2)
      testee.addDependency(dep)

      graph.edgeConnecting(m1, m2).get shouldBe dep
    }

    "merge evidences from different access points" in {
      testee.addModule(m1)
      testee.addModule(m2)
      val dep1 = TrackingModuleDependency(m1, m2)
      dep1.addEvidence(ap1, ap2)
      val dep2 = TrackingModuleDependency(m1, m2)
      dep2.addEvidence(ap2, ap3)

      testee.addDependency(dep1)
      testee.addDependency(dep2)

      val edge = graph.edgeConnecting(m1, m2).get
      edge.destinationsFromSource(ap1) should contain theSameElementsAs List(ap2)
      edge.destinationsFromSource(ap2) should contain theSameElementsAs List(ap3)
      edge.destinations should contain theSameElementsAs List(ap2, ap3)
      edge.sources should contain theSameElementsAs List(ap1, ap2)
    }

    "not add evidences already present" in {
      testee.addModule(m1)
      testee.addModule(m2)
      val dep1 = TrackingModuleDependency(m1, m2)
      dep1.addEvidence(ap1, ap2)
      val dep2 = TrackingModuleDependency(m1, m2)
      dep2.addEvidence(ap1, ap2)

      testee.addDependency(dep1)
      testee.addDependency(dep2)

      val edge = graph.edgeConnecting(m1, m2).get
      edge.destinationsFromSource(ap1) should contain theSameElementsAs List(ap2)
      edge.destinations should contain theSameElementsAs List(ap2)
      edge.sources should contain theSameElementsAs List(ap1)
    }

    "merge evidences from the same access point" in {
      testee.addModule(m1)
      testee.addModule(m2)
      val dep1 = TrackingModuleDependency(m1, m2)
      dep1.addEvidence(ap1, ap2)
      val dep2 = TrackingModuleDependency(m1, m2)
      dep2.addEvidence(ap1, ap3)

      testee.addDependency(dep1)
      testee.addDependency(dep2)

      val edge = graph.edgeConnecting(m1, m2).get
      edge.destinationsFromSource(ap1) should contain theSameElementsAs List(ap2, ap3)
      edge.destinations should contain theSameElementsAs List(ap2, ap3)
      edge.sources should contain theSameElementsAs List(ap1)
    }

    "keep evidence sperated from different modules" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      val dep1 = TrackingModuleDependency(m1, m2)
      dep1.addEvidence(ap1, ap2)
      val dep2 = TrackingModuleDependency(m1, m3)
      dep2.addEvidence(ap1, ap3)

      testee.addDependency(dep1)
      testee.addDependency(dep2)

      val edge1 = graph.edgeConnecting(m1, m2).get
      edge1.sources should contain theSameElementsAs List(ap1)
      edge1.destinations should contain theSameElementsAs List(ap2)

      val edge2 = graph.edgeConnecting(m1, m3).get
      edge2.sources should contain theSameElementsAs List(ap1)
      edge2.destinations should contain theSameElementsAs List(ap3)
    }

    "not add dependency if one of the nodes does not exist" in {
      testee.addModule(m1)
      testee.addModule(m2)
      val dep = TrackingModuleDependency(m1, m3)
      testee.addDependency(dep)

      graph.edges().isEmpty shouldBe true
    }
  }

  "dependencies" should {
    "return all connected modules" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      val dep1 = TrackingModuleDependency(m1, m2)
      val dep2 = TrackingModuleDependency(m1, m3)
      testee.addDependency(dep1)
      testee.addDependency(dep2)

      testee.dependencies(m1) should contain theSameElementsAs List(m2, m3)
      testee.dependencies(m2).isEmpty shouldBe true
    }
  }

  "findDependency" should {
    "return expected dependency" in {
      testee.addModule(m1)
      testee.addModule(m2)
      val dep = TrackingModuleDependency(m1, m2)
      dep.addEvidence(ap1, ap2)

      testee.addDependency(dep)

      testee.findDependency(m1, m2).get shouldBe dep
    }

    "return empty if dependency not defined" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      val dep = TrackingModuleDependency(m1, m2)
      dep.addEvidence(ap1, ap2)

      testee.addDependency(dep)

      testee.findDependency(m1, m3).isDefined shouldBe false
    }
  }
}
