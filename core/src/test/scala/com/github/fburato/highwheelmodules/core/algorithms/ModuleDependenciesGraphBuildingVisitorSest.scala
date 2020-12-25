package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.core.externaladapters.GuavaModuleGraph
import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, ElementName}
import com.github.fburato.highwheelmodules.model.modules.{AnonymousModule, HWModule, ModuleDependency}
import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class ModuleDependenciesGraphBuildingVisitorSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val SUPER_MODULE = HWModule.make("SuperModule", Seq("org.example.*")).get
  private val CORE = HWModule.make("Core", Seq("org.example.core.*")).get
  private val IO = HWModule.make("IO", Seq("org.example.io.*")).get
  private val OTHER = HWModule.make("Other", Seq("")).get
  private val modules = List(CORE, IO, SUPER_MODULE)
  private val graph: MutableNetwork[HWModule, ModuleDependency] = NetworkBuilder.directed().build()
  private val moduleGraph = new GuavaModuleGraph(graph)

  private def makeTestee(whiteList: Option[String], blackList: Option[String]) =
    ModuleDependenciesGraphBuildingVisitor[ModuleDependency](modules, moduleGraph, OTHER,
      (m1, m2, _, _, _) => ModuleDependency(m1, m2), whiteList.flatMap(wl => AnonymousModule.make(Seq(wl))),
      blackList.flatMap(bl => AnonymousModule.make(Seq(bl)))
    )

  private val testee = makeTestee(None, None)

  private def ap(source: String): AccessPoint = AccessPoint(ElementName.fromString(source))


  "construction" should {
    "add all modules to the modules graph" in {
      graph.nodes().asScala should contain theSameElementsAs (OTHER :: modules)
    }
  }

  "apply" should {
    "ignore operation if source matches black list" in {
      val testee = makeTestee(None, Some("*foo*"))

      testee.apply(ap("org.example.core.foo"), ap("org.example.io.FileReader"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if destination matches black list" in {
      val testee = makeTestee(None, Some("*foo*"))

      testee.apply(ap("org.example.core.Test"), ap("org.example.io.foo"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if source does not match white list" in {
      val testee = makeTestee(Some("*bar*"), None)

      testee.apply(ap("org.example.core.Test"), ap("org.example.io.bar"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if destination does not match white list" in {
      val testee = makeTestee(Some("*bar*"), None)

      testee.apply(ap("org.example.core.bar"), ap("org.example.io.Test"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if source and dest match white list but source matches black list" in {
      val testee = makeTestee(Some("org.example.*"), Some("*bar*"))

      testee.apply(ap("org.example.core.bar"), ap("org.example.io.Test"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if source and dest match white list but destination matches black list" in {
      val testee = makeTestee(Some("org.example.*"), Some("*bar*"))

      testee.apply(ap("org.example.core.Test"), ap("org.example.io.bar"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if source and dest do not match black list but dest does not match white list" in {
      val testee = makeTestee(Some("org.example.*foo"), Some("*bar*"))

      testee.apply(ap("org.example.core.Test"), ap("org.example.io.Readerfoo"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "ignore operation if source and dest do not match black list but source does not match white list" in {
      val testee = makeTestee(Some("org.example.*foo"), Some("*bar*"))

      testee.apply(ap("org.example.core.Testfoo"), ap("org.example.io.Reader"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "not ignore operation if source and dest do not match black list and match white list" in {
      val testee = makeTestee(Some("org.example.*foo"), Some("*bar*"))

      testee.apply(ap("org.example.core.Testfoo"), ap("org.example.io.Readerfoo"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(true)
    }

    "add source and destination to the appropriate modules" in {
      testee.apply(ap("org.example.core.Service"), ap("org.example.io.FileReader"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(true)
    }

    "not add connection for elements not matching" in {
      testee.apply(ap("NOTORG.example.core.Service"), ap("org.example.io.FileReader"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(false)
    }

    "connect elements not matching to OTHER" in {
      val matchingOther = ap("NOTORG.example.core.Service")
      val matchingIo = ap("org.example.io.FileReader")

      testee.apply(matchingOther, matchingIo, null)
      testee.apply(matchingIo, matchingOther, null)

      moduleGraph.findDependency(OTHER, IO).isDefined should be(true)
      moduleGraph.findDependency(IO, OTHER).isDefined should be(true)
    }

    "not add self dependencies" in {
      testee.apply(ap("org.example.core.Service"), ap("org.example.core.OtherService"), null)

      moduleGraph.findDependency(CORE, CORE).isDefined should be(false)
    }

    "not add self dependency to OTHER" in {
      testee.apply(ap("NOTorg.example.core.Service"), ap("NOTorg.example.core.OtherService"), null)

      moduleGraph.findDependency(OTHER, OTHER).isDefined should be(false)
    }

    "add source and dest to more modules if multiple regexes match" in {
      testee.apply(ap("org.example.core.Service"), ap("org.example.io.Component"), null)

      moduleGraph.findDependency(CORE, IO).isDefined should be(true)
      moduleGraph.findDependency(CORE, SUPER_MODULE).isDefined should be(true)
      moduleGraph.findDependency(SUPER_MODULE, IO).isDefined should be(true)
    }
  }
}
