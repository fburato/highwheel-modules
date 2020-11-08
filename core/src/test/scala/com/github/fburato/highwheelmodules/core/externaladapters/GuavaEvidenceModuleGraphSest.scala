package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, ElementName}
import com.github.fburato.highwheelmodules.model.modules.{EvidenceModuleDependency, HWModule, TrackingModuleDependency}
import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class GuavaEvidenceModuleGraphSest extends AnyWordSpec with Matchers with OneInstancePerTest {
  private val graph: MutableNetwork[HWModule, TrackingModuleDependency] = NetworkBuilder.directed()
    .allowsSelfLoops(true).build()
  private val aux = new GuavaTrackingModuleGraph(graph)
  private val testee = new GuavaEvidenceModuleGraph(aux, None)

  def module(name: String, regex: String): HWModule = HWModule.make(name, regex).get

  private val m1 = module("module a", "A")
  private val m2 = module("module b", "B")
  private val m3 = module("module c", "C")
  private val ap1 = AccessPoint.create(ElementName.fromString("ap1"))
  private val ap2 = AccessPoint.create(ElementName.fromString("ap2"))
  private val ap3 = AccessPoint.create(ElementName.fromString("ap3"))

  "addModule" should {
    "add vertex to network" in {
      testee.addModule(m1)

      graph.nodes().asScala should contain theSameElementsAs List(m1)
    }

    "not add the same vertex multiple times" in {
      testee.addModule(m1)
      testee.addModule(m1)

      graph.nodes().asScala should contain theSameElementsAs List(m1)
    }
  }

  "addDependency" should {
    "add edge to to network" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      val dependency = graph.edgeConnecting(m1, m2).get()

      (dependency.source, dependency.dest) shouldBe ((m1, m2))
    }

    "build tracking dependency with not evidence limit if graph is built without limit" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3))

      val dependency = graph.edgeConnecting(m1, m2).get

      (dependency.source, dependency.dest) shouldBe ((m1, m2))
      dependency.getDestinations.asScala should contain theSameElementsAs List(ap2, ap3)
    }

    "build tracking dependency with evidence limit if graph is built with evidence limit" in {
      val otherTestee = new GuavaEvidenceModuleGraph(aux, Some(1))
      otherTestee.addModule(m1)
      otherTestee.addModule(m2)
      otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))
      otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3))

      val dependency = graph.edgeConnecting(m1, m2).get

      (dependency.source, dependency.dest) shouldBe ((m1, m2))
      dependency.getDestinations.asScala should contain theSameElementsAs List(ap2)
    }

    "keep tracking of dependencies if limit dependency is 0" in {
      val otherTestee = new GuavaEvidenceModuleGraph(aux, Some(0))
      otherTestee.addModule(m1)
      otherTestee.addModule(m2)
      otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))
      otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3))

      val dependency = graph.edgeConnecting(m1, m2).get

      (dependency.source, dependency.dest) shouldBe ((m1, m2))
      dependency.getDestinations.isEmpty shouldBe true
    }
  }

  "addEdge" should {
    "not add edge if one vertex does not exist" in {
      testee.addModule(m1)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      graph.edges().isEmpty shouldBe true
    }

    "add edge to destination mapping" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      graph.edgeConnecting(m1, m2).get.getDestinationsFromSource(ap1).asScala should contain theSameElementsAs List(ap2)

      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3))

      graph.edgeConnecting(m1, m2).get.getDestinationsFromSource(ap1).asScala should contain theSameElementsAs List(ap2, ap3)
    }

    "skip addition if access point already exists" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      graph.edgeConnecting(m1, m2).get.getDestinationsFromSource(ap1).asScala should contain theSameElementsAs List(ap2)

      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      graph.edgeConnecting(m1, m2).get.getDestinationsFromSource(ap1).asScala should contain theSameElementsAs List(ap2)
    }
  }

  "findDependency" should {
    "find edge in existing graph" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      testee.findDependency(m1, m2).get shouldBe new EvidenceModuleDependency(m1, m2, ap1, ap2)
    }

    "not find edge if edge goes in the opposite direction" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      testee.findDependency(m2, m1).isPresent shouldBe false
    }

    "not find edge if edge does not exist" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      testee.findDependency(m1, m3).isPresent shouldBe false
    }
  }

  "dependencies" should {
    "return empty if nothing connected to module" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      testee.dependencies(m2).isEmpty shouldBe true
    }

    "return empty if module is not present" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))

      testee.dependencies(m3).isEmpty shouldBe true
    }

    "return the expected dependencies" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2))
      testee.addDependency(new EvidenceModuleDependency(m1, m3, ap1, ap2))

      testee.dependencies(m1).asScala should contain theSameElementsAs List(m2, m3)
    }
  }
}
