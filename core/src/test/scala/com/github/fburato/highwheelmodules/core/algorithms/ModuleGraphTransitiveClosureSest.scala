package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure.{Difference, PathDifference}
import com.github.fburato.highwheelmodules.core.externaladapters.GuavaModuleGraph
import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleDependency}
import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._


class ModuleGraphTransitiveClosureSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  private val CORE = HWModule.make("Core", "org.example.core.*").get
  private val FACADE = HWModule.make("Facade", "org.example.core.external.*").get
  private val IO = HWModule.make("IO", "org.example.io.*").get
  private val COMMONS = HWModule.make("Commons", "org.example.commons.*").get
  private val ENDPOINTS = HWModule.make("Endpoints", "org.example.endpoints.*").get
  private val MAIN = HWModule.make("Main", "org.example.Main").get

  private val modules = List(CORE, FACADE, IO, COMMONS, ENDPOINTS, MAIN)
  private val graph: MutableNetwork[HWModule, ModuleDependency] = NetworkBuilder.directed().build()
  private val moduleGraph = new GuavaModuleGraph(graph)

  private def makeTestee(): ModuleGraphTransitiveClosure = {
    def dep(source: HWModule, dest: HWModule): ModuleDependency = new ModuleDependency(source, dest)

    modules.foreach(moduleGraph.addModule)
    List(
      dep(CORE, COMMONS),
      dep(FACADE, CORE),
      dep(IO, COMMONS),
      dep(ENDPOINTS, COMMONS),
      dep(ENDPOINTS, FACADE),
      dep(IO, CORE),
      dep(MAIN, ENDPOINTS),
      dep(MAIN, IO),
      dep(MAIN, CORE)
    ).foreach(moduleGraph.addDependency)
    moduleGraph.addDependency(dep(CORE, COMMONS))
    ModuleGraphTransitiveClosure(moduleGraph, modules)
  }

  private val testee = makeTestee()

  val nonDependent = List(
    (COMMONS, COMMONS),
    (CORE, CORE),
    (FACADE, FACADE),
    (IO, IO),
    (ENDPOINTS, ENDPOINTS),
    (MAIN, MAIN),
    (COMMONS, CORE),
    (COMMONS, FACADE),
    (COMMONS, IO),
    (COMMONS, ENDPOINTS),
    (COMMONS, MAIN),
    (CORE, FACADE),
    (CORE, IO),
    (CORE, ENDPOINTS),
    (CORE, MAIN),
    (FACADE, IO),
    (FACADE, ENDPOINTS),
    (FACADE, MAIN),
    (IO, ENDPOINTS),
    (IO, FACADE),
    (IO, MAIN),
    (ENDPOINTS, IO),
    (ENDPOINTS, MAIN)
  )

  val dependent = List(
    (CORE, COMMONS, List(COMMONS)),
    (FACADE, CORE, List(CORE)),
    (FACADE, COMMONS, List(CORE, COMMONS)),
    (IO, COMMONS, List(COMMONS)),
    (IO, CORE, List(CORE)),
    (ENDPOINTS, FACADE, List(FACADE)),
    (ENDPOINTS, CORE, List(FACADE, CORE)),
    (ENDPOINTS, COMMONS, List(COMMONS)),
    (MAIN, CORE, List(CORE)),
    (MAIN, IO, List(IO)),
    (MAIN, ENDPOINTS, List(ENDPOINTS)),
    (MAIN, FACADE, List(ENDPOINTS, FACADE)),
    (MAIN, COMMONS, List(CORE, COMMONS))
  )

  "minimum distance" should {

    nonDependent foreach {
      case (source, dest) => s"be max int for not dependent modules (${source.name}, ${dest.name})" in {
        testee.minimumDistance(source, dest).get shouldEqual Int.MaxValue
      }
    }

    dependent foreach {
      case (source, dest, deps) => s"be ${deps.size} between ${source.name} and ${dest.name}" in {
        testee.minimumDistance(source, dest).get shouldEqual deps.size
      }
    }
  }

  "minimum path distance" should {
    nonDependent foreach {
      case (source, dest) => s"be empty for not dependent modules (${source.name}, ${dest.name})" in {
        testee.minimumDistancePath(source, dest).toList.isEmpty shouldBe true
      }
    }

    dependent foreach {
      case (source, dest, deps) => s"be $deps between ${source.name} and ${dest.name}" in {
        testee.minimumDistancePath(source, dest).toList should contain theSameElementsInOrderAs deps
      }
    }
  }

  "is reachable" should {
    for {
      source <- modules
      dest <- modules
    } yield s"be true iff minimum distance is less than max int (${source.name}, ${dest.name})" in {
      val reachable = testee.isReachable(source, dest)
      val distance = testee.minimumDistance(source, dest).get

      (reachable && distance < Int.MaxValue || !reachable && distance == Int.MaxValue) shouldBe true
    }
    ()
  }

  class TransitiveClosureBuilder(init: TransitiveClosureBuilder => ()) {
    private val modules = new ArrayBuffer[HWModule]()
    private val mutableNetwork: MutableNetwork[HWModule, ModuleDependency] = NetworkBuilder.directed().allowsSelfLoops(true).build()
    private val guavaModuleGraph = new GuavaModuleGraph(mutableNetwork)

    def module(name: String, globs: String*): HWModule = {
      val module = HWModule.make(name, globs.toList.asJava).get
      modules.addOne(module)
      guavaModuleGraph.addModule(module)
      module
    }

    def dependency(source: HWModule, dest: HWModule): ModuleDependency = {
      val dependency = new ModuleDependency(source, dest)
      guavaModuleGraph.addDependency(dependency)
      dependency
    }

    def build(): ModuleGraphTransitiveClosure = {
      init(this)
      ModuleGraphTransitiveClosure(guavaModuleGraph, modules.toSeq)
    }
  }

  private val equalGraphBuilder = new TransitiveClosureBuilder(b => {
    val core2 = b.module("Core", "org.example.core.*")
    val facade2 = b.module("Facade", "org.example.core.external.*")
    val io2 = b.module("IO", "org.example.io.*")
    val commons2 = b.module("Commons", "org.example.commons.*")
    val endpoints2 = b.module("Endpoints", "org.example.endpoints.*")
    val main2 = b.module("Main", "org.example.Main")
    b.dependency(core2, commons2)
    b.dependency(facade2, core2)
    b.dependency(io2, commons2)
    b.dependency(endpoints2, commons2)
    b.dependency(endpoints2, facade2)
    b.dependency(io2, core2)
    b.dependency(main2, endpoints2)
    b.dependency(main2, io2)
    b.dependency(main2, core2)
  })
  private val differentModuleBuilder = new TransitiveClosureBuilder(b => {
    val core2 = b.module("Core", "org.example.core.*")
    val facade2 = b.module("Facade", "org.example.core.external.*")
    val io2 = b.module("IO", "org.example.io.*")
    val commons2 = b.module("Commons", "org.example.commons.*")
    val endpoints2 = b.module("Endpoints", "org.example.endpoints.*")
    val main2 = b.module("NOT_MAIN", "org.example.Main")
    b.dependency(core2, commons2)
    b.dependency(facade2, core2)
    b.dependency(io2, commons2)
    b.dependency(endpoints2, commons2)
    b.dependency(endpoints2, facade2)
    b.dependency(io2, core2)
    b.dependency(main2, endpoints2)
    b.dependency(main2, io2)
    b.dependency(main2, core2)
  })
  private val missingDependencyBuilder = new TransitiveClosureBuilder(b => {
    val core2 = b.module("Core", "org.example.core.*")
    val facade2 = b.module("Facade", "org.example.core.external.*")
    val io2 = b.module("IO", "org.example.io.*")
    val commons2 = b.module("Commons", "org.example.commons.*")
    val endpoints2 = b.module("Endpoints", "org.example.endpoints.*")
    val main2 = b.module("Main", "org.example.Main")
    b.dependency(core2, commons2)
    b.dependency(facade2, core2)
    b.dependency(io2, commons2)
    b.dependency(endpoints2, commons2)
    b.dependency(endpoints2, facade2)
    b.dependency(io2, core2)
    b.dependency(main2, endpoints2)
    b.dependency(main2, io2)
    //b.dependency(main2, core2)
  })

  "same" should {
    "return true if transitive closures are equal" in {
      testee.same(equalGraphBuilder.build()) shouldBe true
    }

    "return false on transitive closure with different modules" in {
      testee.same(differentModuleBuilder.build()) shouldBe false
    }

    "return false on transitive closure with different dependencies" in {
      testee.same(missingDependencyBuilder.build()) shouldBe false
    }
  }

  "diff" should {
    "return empty list if transitive closures are equal" in {
      testee.diff(equalGraphBuilder.build()).get.isEmpty shouldBe true
    }

    "return return None on module graphs with different modules" in {
      testee.diff(differentModuleBuilder.build()).isDefined shouldBe false
    }

    "return the expected differences for missing dependency" in {
      val diffs = testee.diff(missingDependencyBuilder.build()).get

      diffs should contain theSameElementsAs List(Difference(MAIN, CORE, 1, 2))
    }
  }

  "diffPath" should {
    "return empty path if transitive closures are equal" in {
      testee.diffPath(equalGraphBuilder.build()).get.isEmpty shouldBe true
    }

    "return None on module graphs with different modules" in {
      testee.diffPath(differentModuleBuilder.build()).isDefined shouldBe false
    }

    "return the expected differences for missinig dependency" in {
      val diffs = testee.diffPath(missingDependencyBuilder.build()).get

      diffs should contain theSameElementsAs List(PathDifference(MAIN, CORE, Seq(CORE), Seq(IO, CORE)))
    }
  }
}
