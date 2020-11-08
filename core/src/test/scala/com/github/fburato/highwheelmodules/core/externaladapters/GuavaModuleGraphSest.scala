package com.github.fburato.highwheelmodules.core.externaladapters

import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleDependency}
import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class GuavaModuleGraphSest extends AnyWordSpec with Matchers with OneInstancePerTest {
  private val graph: MutableNetwork[HWModule, ModuleDependency] = NetworkBuilder.directed().allowsSelfLoops(true).build()
  private val testee = new GuavaModuleGraph(graph)

  def module(name: String, regex: String): HWModule = HWModule.make(name, regex).get

  private val m1 = module("module a", "A")
  private val m2 = module("module b", "B")
  private val m3 = module("module c", "C")

  "addModule" should {
    "add vertex to network" in {
      testee.addModule(m1)

      graph.nodes().asScala should contain(m1)
    }

    "add vertex should not add same module twice" in {
      testee.addModule(m1)
      testee.addModule(m1)

      graph.nodes().asScala should contain theSameElementsAs List(m1)
    }
  }

  "addDependency" should {
    "add edge to network" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      val dependency = graph.edgeConnecting(m1, m2).get
      (dependency.source, dependency.dest) shouldBe ((m1, m2))
    }

    "not add edge if one of nodes does not exist" in {
      testee.addModule(m1)
      testee.addDependency(new ModuleDependency(m1, m2))

      graph.edges().isEmpty shouldBe true
    }

    "increase counter if dependency added" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      graph.edgeConnecting(m1, m2).get.getCount shouldBe 1
    }

    "increase counter multiple times if dependency added multiple times" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m1, m2))

      graph.edgeConnecting(m1, m2).get.getCount shouldBe 2
    }
  }

  "findEdge" should {
    "find edge in existing graph" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      val dependency = testee.findDependency(m1, m2).get
      (dependency.source, dependency.dest) shouldBe ((m1, m2))
    }

    "return empty if edge goes in the opposite direction" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      testee.findDependency(m2, m1).isPresent shouldBe false
    }

    "return empty if edge does not exist" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      testee.findDependency(m1, m3).isPresent shouldBe false
    }
  }

  "fanIn of module" should {
    "be empty if module is not in graph" in {
      testee.addModule(m1)

      testee.fanInOf(m2).isPresent shouldBe false
    }

    "equal the amount of incoming edges" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      testee.addDependency(new ModuleDependency(m2, m1))
      testee.addDependency(new ModuleDependency(m3, m1))

      testee.fanInOf(m1).get shouldBe 2
    }

    "ignore multiple dependencies" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m1, m2))

      testee.fanInOf(m2).get shouldBe 1
    }

    "ignore self references" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m2, m2))

      testee.fanInOf(m2).get shouldBe 1
    }
  }

  "fanOut of module" should {
    "be empty if module is not in graph" in {
      testee.addModule(m1)

      testee.fanOutOf(m2).isPresent shouldBe false
    }

    "equal the amount of outgoing edges" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m1, m3))

      testee.fanOutOf(m1).get shouldBe 2
    }

    "ignore multiple dependencies" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m1, m2))

      testee.fanOutOf(m1).get shouldBe 1
    }

    "ignore self references" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m1, m1))

      testee.fanOutOf(m1).get shouldBe 1
    }
  }

  "dependencies" should {
    "return empty collection if nothing connected to module" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      testee.dependencies(m2).isEmpty shouldBe true
    }

    "return empty collection if module not present" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addDependency(new ModuleDependency(m1, m2))

      testee.dependencies(m3).isEmpty shouldBe true
    }

    "return collection of expected dependencies" in {
      testee.addModule(m1)
      testee.addModule(m2)
      testee.addModule(m3)
      testee.addDependency(new ModuleDependency(m1, m2))
      testee.addDependency(new ModuleDependency(m1, m3))

      testee.dependencies(m1).asScala.toList should contain theSameElementsAs List(m2, m3)
    }
  }
}
