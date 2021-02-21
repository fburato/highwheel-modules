package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.bytecodeparser.ClassPathParser
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.DirectoryClassPathRoot
import com.github.fburato.highwheelmodules.core.externaladapters.GuavaGraphFactory
import com.github.fburato.highwheelmodules.model.analysis.{LOOSE, STRICT}
import com.github.fburato.highwheelmodules.model.classpath.{ClassParser, ClasspathRoot}
import com.github.fburato.highwheelmodules.model.modules.{
  AnonymousModule,
  Definition,
  HWModule,
  ModuleGraphFactory
}
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Paths
import scala.util.Failure

class ModuleAnalyserTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {

  private val orgExamples = new DirectoryClassPathRoot(
    Paths.get("core", "target", "scala-2.13", "test-classes").toFile
  )
  private val realClassParser: ClassParser = new ClassPathParser(item =>
    item.asJavaName startsWith "org.example"
  )
  private val classParser = spy(realClassParser)
  private val factory: ModuleGraphFactory = new GuavaGraphFactory

  private val MAIN = HWModule.make("Main", Seq("org.example.Main")).get
  private val CONTROLLER = HWModule.make("Controllers", Seq("org.example.controller.*")).get
  private val FACADE = HWModule.make("Facade", Seq("org.example.core.CoreFacade")).get
  private val COREINTERNALS =
    HWModule.make("CoreInternals", Seq("org.example.core.internals.*")).get
  private val COREAPI = HWModule.make("CoreApi", Seq("org.example.core.api.*")).get
  private val MODEL = HWModule.make("Model", Seq("org.example.core.model.*")).get
  private val IO = HWModule.make("IO", Seq("org.example.io.*")).get
  private val UTILS = HWModule.make("Commons", Seq("org.example.commons.*")).get

  private def testee(root: ClasspathRoot, evidenceLimit: Option[Int]): ModuleAnalyser =
    ModuleAnalyser(classParser, root, evidenceLimit, factory)

  private def metric(name: String, fanIn: Int, fanOut: Int): Metric = Metric(name, fanIn, fanOut)

  private def dep(source: HWModule, dest: HWModule): DependencyS = DependencyS(source, dest)

  private def noSD(source: HWModule, dest: HWModule): NoStrictDependencyS =
    NoStrictDependencyS(source, dest)

  private def violation(
    source: String,
    dest: String,
    specPath: Seq[String],
    actualPath: Seq[String]
  ): EvidenceBackedViolation =
    EvidenceBackedViolation(source, dest, specPath, actualPath, List())

  private def violation(source: String, dest: String): ModuleConnectionViolation =
    ModuleConnectionViolation(source, dest)

  private def evidence(
    source: String,
    dest: String,
    specPath: Seq[String],
    actualPath: Seq[String],
    evidence: Seq[Seq[(String, String)]]
  ) =
    EvidenceBackedViolation(source, dest, specPath, actualPath, evidence)

  "when definitions are all strict, analyse" should {

    val strictBuilder = Definition(mode = STRICT)

    "analyse if sepcfication includes only one module and no rules" in {
      val definition = strictBuilder.copy(modules = List(MAIN))
      val actual = testee(orgExamples, None).analyse(Seq(definition)).get.head

      actual.evidenceBackedViolations.size shouldEqual 0
      actual.moduleConnectionViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(metric(MAIN.name, 0, 0))
    }

    "fail if no module is provided" in {
      val definition = strictBuilder
      testee(orgExamples, None).analyse(Seq(definition)) should matchPattern {
        case Failure(_: AnalyserException) =>
      }
    }

    "analyse specification with many modules and rules" in {
      val definition = strictBuilder.copy(
        modules = List(MAIN, CONTROLLER),
        dependencies = List(dep(MAIN, CONTROLLER)),
        noStrictDependencies = List(noSD(CONTROLLER, MAIN))
      )

      val actual = testee(orgExamples, None).analyse(Seq(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs Seq(
        metric(MAIN.name, 0, 1),
        metric(CONTROLLER.name, 1, 0)
      )
    }

    "detect violations on specification" in {
      val definition = strictBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE),
        dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)),
        noStrictDependencies = List(noSD(MAIN, FACADE))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations should contain allElementsOf List(
        violation(MAIN.name, MAIN.name, List(CONTROLLER.name, MAIN.name), List())
      )
      actual.moduleConnectionViolations should contain theSameElementsAs List(
        violation(MAIN.name, FACADE.name)
      )
      actual.metrics should contain theSameElementsAs List(
        metric(MAIN.name, 0, 2),
        metric(CONTROLLER.name, 1, 1),
        metric(FACADE.name, 2, 0)
      )
    }

    "provide evidence for dependency violations" in {
      val definition = strictBuilder.copy(modules = List(MAIN, CONTROLLER, FACADE))

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations should contain allElementsOf List(
        evidence(
          MAIN.name,
          CONTROLLER.name,
          List(),
          List(CONTROLLER.name),
          List(
            List(
              ("org.example.Main:main", "org.example.controller.Controller1:(init)"),
              ("org.example.Main:main", "org.example.controller.Controller1:access")
            )
          )
        ),
        evidence(
          MAIN.name,
          FACADE.name,
          List(),
          List(FACADE.name),
          List(List(("org.example.Main:main", "org.example.core.CoreFacade:(init)")))
        ),
        evidence(
          CONTROLLER.name,
          FACADE.name,
          List(),
          List(FACADE.name),
          List(
            List(
              ("org.example.controller.Controller1", "org.example.core.CoreFacade"),
              ("org.example.controller.Controller1:(init)", "org.example.core.CoreFacade"),
              (
                "org.example.controller.Controller1:access",
                "org.example.core.CoreFacade:facadeMethod1"
              )
            )
          )
        )
      )
    }

    "limit evidence for dependency violations if evidence limit configured" in {
      val definition = strictBuilder.copy(modules = List(MAIN, CONTROLLER, FACADE))

      val actual = testee(orgExamples, Some(1)).analyse(List(definition)).get.head

      val mainControllersEvidence = actual.evidenceBackedViolations
        .filter(v => v.sourceModule == MAIN.name && v.destinationModule == CONTROLLER.name)
        .head
        .evidences
      List(
        ("org.example.Main:main", "org.example.controller.Controller1:access"),
        ("org.example.Main:main", "org.example.controller.Controller1"),
        ("org.example.Main:main", "org.example.controller.Controller1:(init)")
      ) should contain allElementsOf mainControllersEvidence.head
      mainControllersEvidence.size shouldEqual 1

      val mainFacadeEvidence = actual.evidenceBackedViolations
        .filter(v => v.sourceModule == MAIN.name && v.destinationModule == FACADE.name)
        .head
        .evidences
      List(
        ("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
        ("org.example.Main:main", "org.example.core.CoreFacade")
      ) should contain allElementsOf mainFacadeEvidence.head

      val controllersFacade = actual.evidenceBackedViolations
        .filter(v => v.sourceModule == CONTROLLER.name && v.destinationModule == FACADE.name)
        .head
        .evidences
      List(
        ("org.example.controller.Controller1:access", "org.example.core.CoreFacade:facadeMethod1"),
        ("org.example.controller.Controller1", "org.example.core.CoreFacade"),
        ("org.example.controller.Controller1:(init)", "org.example.core.CoreFacade")
      ) should contain allElementsOf controllersFacade.head

    }

    "take all modules in scope" in {
      val definition = strictBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(MAIN, CONTROLLER),
          dep(MAIN, FACADE),
          dep(MAIN, COREAPI),
          dep(MAIN, IO),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(COREINTERNALS, UTILS),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, COREAPI),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL),
          dep(IO, UTILS)
        ),
        noStrictDependencies =
          List(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS), noSD(IO, COREINTERNALS))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations.size shouldEqual 0
      actual.moduleConnectionViolations.size shouldEqual 0
      actual.metrics should contain allElementsOf List(
        metric(MAIN.name, 0, 4),
        metric(CONTROLLER.name, 1, 1),
        metric(FACADE.name, 2, 3),
        metric(COREAPI.name, 3, 1),
        metric(COREINTERNALS.name, 1, 2),
        metric(IO.name, 1, 3),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 2, 0)
      )
    }

    "consider only dependencies in the whitelist" in {
      val definition = strictBuilder.copy(
        whitelist = AnonymousModule.make(Seq("org.example.Main", "org.example.controller.*")),
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(dep(MAIN, CONTROLLER))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations.size shouldEqual 0
      actual.moduleConnectionViolations.size shouldEqual 0
      actual.metrics should contain allElementsOf List(
        metric(MAIN.name, 0, 1),
        metric(CONTROLLER.name, 1, 0),
        metric(FACADE.name, 0, 0),
        metric(COREAPI.name, 0, 0),
        metric(COREINTERNALS.name, 0, 0),
        metric(IO.name, 0, 0),
        metric(MODEL.name, 0, 0),
        metric(UTILS.name, 0, 0)
      )
    }

    "consider dependencies not in the blacklist" in {
      val definition = strictBuilder.copy(
        blacklist = AnonymousModule.make(Seq("org.example.Main", "org.example.commons.*")),
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, COREAPI),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)
        ),
        noStrictDependencies = List(noSD(CONTROLLER, COREINTERNALS), noSD(IO, COREINTERNALS))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations.size shouldEqual 0
      actual.moduleConnectionViolations.size shouldEqual 0
      actual.metrics should contain allElementsOf List(
        metric(MAIN.name, 0, 0),
        metric(CONTROLLER.name, 0, 1),
        metric(FACADE.name, 1, 3),
        metric(COREAPI.name, 2, 1),
        metric(COREINTERNALS.name, 1, 1),
        metric(IO.name, 0, 2),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 0, 0)
      )
    }

    "consider dependencies not in the blacklist and in the whitelist" in {
      val definition = strictBuilder.copy(
        whitelist = AnonymousModule.make(Seq("org.example.*")),
        blacklist = AnonymousModule.make(Seq("org.example.Main", "org.example.commons.*")),
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, COREAPI),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)
        ),
        noStrictDependencies = List(noSD(CONTROLLER, COREINTERNALS), noSD(IO, COREINTERNALS))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations.size shouldEqual 0
      actual.moduleConnectionViolations.size shouldEqual 0
      actual.metrics should contain allElementsOf List(
        metric(MAIN.name, 0, 0),
        metric(CONTROLLER.name, 0, 1),
        metric(FACADE.name, 1, 3),
        metric(COREAPI.name, 2, 1),
        metric(COREINTERNALS.name, 1, 1),
        metric(IO.name, 0, 2),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 0, 0)
      )
    }

    "perform analysis of multiple definitions" in {
      val definition1 = strictBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE),
        dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)),
        noStrictDependencies = List(noSD(MAIN, FACADE))
      )
      val definition2 = strictBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(MAIN, CONTROLLER),
          dep(MAIN, FACADE),
          dep(MAIN, COREAPI),
          dep(MAIN, IO),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(COREINTERNALS, UTILS),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, COREAPI),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL),
          dep(IO, UTILS)
        ),
        noStrictDependencies =
          List(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS), noSD(IO, COREINTERNALS))
      )

      val (actual1, actual2) =
        testee(orgExamples, None).analyse(List(definition1, definition2)).get match {
          case Seq(a1, a2) => (a1, a2)
        }

      verify(classParser).parse(refEq(orgExamples), any)

      actual1.evidenceBackedViolations should contain allElementsOf List(
        violation(MAIN.name, MAIN.name, List(CONTROLLER.name, MAIN.name), List()),
        violation(CONTROLLER.name, MAIN.name, List(MAIN.name), List())
      )
      actual1.moduleConnectionViolations should contain allElementsOf List(
        violation(MAIN.name, FACADE.name)
      )
      actual1.metrics should contain theSameElementsAs List(
        metric("Main", 0, 2),
        metric("Controllers", 1, 1),
        metric("Facade", 2, 0)
      )

      actual2.evidenceBackedViolations.size shouldEqual 0
      actual2.moduleConnectionViolations.size shouldEqual 0
      actual2.metrics should contain theSameElementsAs List(
        metric(MAIN.name, 0, 4),
        metric(CONTROLLER.name, 1, 1),
        metric(FACADE.name, 2, 3),
        metric(COREAPI.name, 3, 1),
        metric(COREINTERNALS.name, 1, 2),
        metric(IO.name, 1, 3),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 2, 0)
      )
    }
  }

  "when definitions are all loose, analyse" should {

    val looseBuilder = Definition(mode = LOOSE)

    "analyse if specification has one module and no rule" in {
      val definition = looseBuilder.copy(modules = List(MAIN))

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(metric(MAIN.name, 0, 0))
    }

    "fail if no module is provided" in {
      val definition = looseBuilder.copy()
      testee(orgExamples, None).analyse(Seq(definition)) should matchPattern {
        case Failure(_: AnalyserException) =>
      }
    }

    "analyse specification with many modules and rules" in {
      val definition = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER),
        dependencies = List(dep(MAIN, CONTROLLER)),
        noStrictDependencies = List(noSD(CONTROLLER, MAIN))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(
        metric("Main", 0, 1),
        metric("Controllers", 1, 0)
      )
    }

    "detect violations on the specification" in {
      val definition = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE),
        dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)),
        noStrictDependencies = List(noSD(MAIN, FACADE))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations should contain allElementsOf List(
        violation(CONTROLLER.name, MAIN.name)
      )
      actual.evidenceBackedViolations should contain allElementsOf List(
        evidence(
          MAIN.name,
          FACADE.name,
          List(MAIN.name, FACADE.name),
          List(FACADE.name),
          List(List(("org.example.Main:main", "org.example.core.CoreFacade:(init)")))
        )
      )
      actual.metrics should contain theSameElementsAs List(
        metric("Main", 0, 2),
        metric("Controllers", 1, 1),
        metric("Facade", 2, 0)
      )
    }

    "provide evidence for undesired dependencies" in {
      val definition = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE),
        noStrictDependencies = List(noSD(MAIN, CONTROLLER))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations should contain allElementsOf List(
        evidence(
          MAIN.name,
          CONTROLLER.name,
          List(MAIN.name, CONTROLLER.name),
          List(CONTROLLER.name),
          List(
            List(
              ("org.example.Main:main", "org.example.controller.Controller1:(init)"),
              ("org.example.Main:main", "org.example.controller.Controller1:access")
            )
          )
        )
      )
    }

    "limit evidence provided if limit configured" in {
      val definition = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE),
        noStrictDependencies = List(noSD(MAIN, CONTROLLER))
      )

      val actual = testee(orgExamples, Some(1)).analyse(List(definition)).get.head
      val mainController = actual.evidenceBackedViolations
        .filter(v => v.sourceModule == MAIN.name && v.destinationModule == CONTROLLER.name)
        .head
      List(
        ("org.example.Main:main", "org.example.controller.Controller1:access"),
        ("org.example.Main:main", "org.example.controller.Controller1"),
        ("org.example.Main:main", "org.example.controller.Controller1:(init)")
      ) should contain allElementsOf List(mainController.evidences.head.head)
      mainController.evidences.head.size shouldEqual 1
    }

    "analyse all modules in scope" in {
      val definition = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(MAIN, CONTROLLER),
          dep(MAIN, IO),
          dep(MAIN, MODEL),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)
        ),
        noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain allElementsOf List(
        metric(MAIN.name, 0, 4),
        metric(CONTROLLER.name, 1, 1),
        metric(FACADE.name, 2, 3),
        metric(COREAPI.name, 3, 1),
        metric(COREINTERNALS.name, 1, 2),
        metric(IO.name, 1, 3),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 2, 0)
      )
    }

    "consider only dependencies in the whitelist" in {
      val definition = looseBuilder.copy(
        whitelist = AnonymousModule.make(
          Seq("org.example.controller.*", "org.example.core.CoreFacade", "org.example.core.model.*")
        ),
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(dep(CONTROLLER, FACADE), dep(FACADE, MODEL)),
        noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(
        metric(MAIN.name, 0, 0),
        metric(CONTROLLER.name, 0, 1),
        metric(FACADE.name, 1, 1),
        metric(COREAPI.name, 0, 0),
        metric(COREINTERNALS.name, 0, 0),
        metric(IO.name, 0, 0),
        metric(MODEL.name, 1, 0),
        metric(UTILS.name, 0, 0)
      )
    }

    "consider dependencies not in the blacklist" in {
      val definition = looseBuilder.copy(
        blacklist = AnonymousModule.make(Seq("org.example.Main", "org.example.commons.*")),
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)
        ),
        noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN))
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(
        metric(MAIN.name, 0, 0),
        metric(CONTROLLER.name, 0, 1),
        metric(FACADE.name, 1, 3),
        metric(COREAPI.name, 2, 1),
        metric(COREINTERNALS.name, 1, 1),
        metric(IO.name, 0, 2),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 0, 0)
      )
    }

    "consider dependencies in the whitelist and not in the blacklist" in {
      val definition = looseBuilder.copy(
        whitelist = AnonymousModule.make(Seq("org.example.*")),
        blacklist = AnonymousModule.make(Seq("org.example.Main", "org.example.commons.*")),
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)
        )
      )

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain allElementsOf List(
        metric(MAIN.name, 0, 0),
        metric(CONTROLLER.name, 0, 1),
        metric(FACADE.name, 1, 3),
        metric(COREAPI.name, 2, 1),
        metric(COREINTERNALS.name, 1, 1),
        metric(IO.name, 0, 2),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 0, 0)
      )
    }

    "analyse multiple definitions in one pass" in {
      val definition1 = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE),
        dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)),
        noStrictDependencies = List(noSD(MAIN, FACADE))
      )
      val definition2 = looseBuilder.copy(
        modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        dependencies = List(
          dep(MAIN, CONTROLLER),
          dep(MAIN, IO),
          dep(MAIN, MODEL),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)
        ),
        noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN))
      )

      val (actual1, actual2) =
        testee(orgExamples, None).analyse(List(definition1, definition2)).get match {
          case Seq(a1, a2) => (a1, a2)
        }

      verify(classParser).parse(refEq(orgExamples), any)

      actual1.moduleConnectionViolations should contain allElementsOf List(
        violation(CONTROLLER.name, MAIN.name)
      )
      actual1.evidenceBackedViolations should contain allElementsOf List(
        evidence(
          MAIN.name,
          FACADE.name,
          List(MAIN.name, FACADE.name),
          List(FACADE.name),
          List(List(("org.example.Main:main", "org.example.core.CoreFacade:(init)")))
        )
      )
      actual1.metrics should contain theSameElementsAs List(
        metric("Main", 0, 2),
        metric("Controllers", 1, 1),
        metric("Facade", 2, 0)
      )

      actual2.moduleConnectionViolations.size shouldEqual 0
      actual2.evidenceBackedViolations.size shouldEqual 0
      actual2.metrics should contain theSameElementsAs List(
        metric(MAIN.name, 0, 4),
        metric(CONTROLLER.name, 1, 1),
        metric(FACADE.name, 2, 3),
        metric(COREAPI.name, 3, 1),
        metric(COREINTERNALS.name, 1, 2),
        metric(IO.name, 1, 3),
        metric(MODEL.name, 4, 0),
        metric(UTILS.name, 2, 0)
      )
    }
  }

  "analyse should change mode based on definition" in {
    val definition1 = Definition(
      mode = STRICT,
      modules = List(MAIN, CONTROLLER, FACADE),
      dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)),
      noStrictDependencies = List(noSD(MAIN, FACADE))
    )
    val definition2 = Definition(
      mode = STRICT,
      modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
      dependencies = List(
        dep(MAIN, CONTROLLER),
        dep(MAIN, FACADE),
        dep(MAIN, COREAPI),
        dep(MAIN, IO),
        dep(CONTROLLER, FACADE),
        dep(COREINTERNALS, MODEL),
        dep(COREINTERNALS, UTILS),
        dep(FACADE, COREINTERNALS),
        dep(FACADE, COREAPI),
        dep(FACADE, MODEL),
        dep(COREAPI, MODEL),
        dep(IO, COREAPI),
        dep(IO, MODEL),
        dep(IO, UTILS)
      ),
      noStrictDependencies =
        List(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS), noSD(IO, COREINTERNALS))
    )
    val definition3 = Definition(
      mode = LOOSE,
      modules = List(MAIN, CONTROLLER, FACADE),
      dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)),
      noStrictDependencies = List(noSD(MAIN, FACADE))
    )
    val definition4 = Definition(
      mode = LOOSE,
      modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
      dependencies = List(
        dep(MAIN, CONTROLLER),
        dep(MAIN, IO),
        dep(MAIN, MODEL),
        dep(CONTROLLER, FACADE),
        dep(COREINTERNALS, MODEL),
        dep(FACADE, COREINTERNALS),
        dep(FACADE, MODEL),
        dep(COREAPI, MODEL),
        dep(IO, COREAPI),
        dep(IO, MODEL)
      ),
      noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN))
    )

    val (actual1, actual2, actual3, actual4) = testee(orgExamples, None)
      .analyse(List(definition1, definition2, definition3, definition4))
      .get match {
      case Seq(a1, a2, a3, a4) => (a1, a2, a3, a4)
    }

    verify(classParser).parse(refEq(orgExamples), any)

    actual1.evidenceBackedViolations should contain allElementsOf List(
      violation(MAIN.name, MAIN.name, List(CONTROLLER.name, MAIN.name), List()),
      violation(CONTROLLER.name, MAIN.name, List(MAIN.name), List())
    )
    actual1.moduleConnectionViolations should contain allElementsOf List(
      violation(MAIN.name, FACADE.name)
    )
    actual1.metrics should contain theSameElementsAs List(
      metric("Main", 0, 2),
      metric("Controllers", 1, 1),
      metric("Facade", 2, 0)
    )

    actual2.evidenceBackedViolations.size shouldEqual 0
    actual2.moduleConnectionViolations.size shouldEqual 0
    actual2.metrics should contain theSameElementsAs List(
      metric(MAIN.name, 0, 4),
      metric(CONTROLLER.name, 1, 1),
      metric(FACADE.name, 2, 3),
      metric(COREAPI.name, 3, 1),
      metric(COREINTERNALS.name, 1, 2),
      metric(IO.name, 1, 3),
      metric(MODEL.name, 4, 0),
      metric(UTILS.name, 2, 0)
    )

    actual3.moduleConnectionViolations should contain allElementsOf List(
      violation(CONTROLLER.name, MAIN.name)
    )
    actual3.evidenceBackedViolations should contain allElementsOf List(
      evidence(
        MAIN.name,
        FACADE.name,
        List(MAIN.name, FACADE.name),
        List(FACADE.name),
        List(List(("org.example.Main:main", "org.example.core.CoreFacade:(init)")))
      )
    )
    actual3.metrics should contain theSameElementsAs List(
      metric("Main", 0, 2),
      metric("Controllers", 1, 1),
      metric("Facade", 2, 0)
    )

    actual4.moduleConnectionViolations.size shouldEqual 0
    actual4.evidenceBackedViolations.size shouldEqual 0
    actual4.metrics should contain theSameElementsAs List(
      metric(MAIN.name, 0, 4),
      metric(CONTROLLER.name, 1, 1),
      metric(FACADE.name, 2, 3),
      metric(COREAPI.name, 3, 1),
      metric(COREINTERNALS.name, 1, 2),
      metric(IO.name, 1, 3),
      metric(MODEL.name, 4, 0),
      metric(UTILS.name, 2, 0)
    )
  }
}
