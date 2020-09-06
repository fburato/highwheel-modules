package com.github.fburato.highwheelmodules.core.analysis

import java.nio.file.Paths

import com.github.fburato.highwheelmodules.bytecodeparser.ClassPathParser
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.DirectoryClassPathRoot
import com.github.fburato.highwheelmodules.core.externaladapters.GuavaGraphFactory
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode
import com.github.fburato.highwheelmodules.model.classpath.{ClassParser, ClasspathRoot}
import com.github.fburato.highwheelmodules.model.modules.{AnonymousModule, Definition, HWModule, ModuleGraphFactory}
import com.github.fburato.highwheelmodules.model.rules.{Dependency, NoStrictDependency}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._
import scala.util.Failure

class ModuleAnalyserSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val orgExamples = new DirectoryClassPathRoot(Paths.get("target", "test-classes").toFile)
  private val realClassParser: ClassParser = new ClassPathParser(item => item.asJavaName() startsWith "org.example")
  private val classParser = spy(realClassParser)
  private val factory: ModuleGraphFactory = new GuavaGraphFactory

  private val MAIN = HWModule.make("Main", "org.example.Main").get()
  private val CONTROLLER = HWModule.make("Controllers", "org.example.controller.*").get()
  private val FACADE = HWModule.make("Facade", "org.example.core.CoreFacade").get()
  private val COREINTERNALS = HWModule.make("CoreInternals", "org.example.core.internals.*").get()
  private val COREAPI = HWModule.make("CoreApi", "org.example.core.api.*").get()
  private val MODEL = HWModule.make("Model", "org.example.core.model.*").get()
  private val IO = HWModule.make("IO", "org.example.io.*").get()
  private val UTILS = HWModule.make("Commons", "org.example.commons.*").get()

  private def testee(root: ClasspathRoot, evidenceLimit: Option[Int]): ModuleAnalyser =
    ModuleAnalyser(classParser, root, evidenceLimit, factory)

  private def metric(name: String, fanIn: Int, fanOut: Int): Metric = Metric(name, fanIn, fanOut)

  private def dep(source: HWModule, dest: HWModule): Dependency = new Dependency(source, dest)

  private def noSD(source: HWModule, dest: HWModule): NoStrictDependency = new NoStrictDependency(source, dest)

  private def violation(source: String, dest: String, specPath: Seq[String], actualPath: Seq[String]): EvidenceBackedViolation =
    EvidenceBackedViolation(source, dest, specPath, actualPath, List())

  private def violation(source: String, dest: String): ModuleConnectionViolation =
    ModuleConnectionViolation(source, dest)

  private def evidence(source: String, dest: String, specPath: Seq[String], actualPath: Seq[String], evidence: Seq[Seq[(String, String)]]) =
    EvidenceBackedViolation(source, dest, specPath, actualPath, evidence)

  "when definitions are all strict, analyse" should {

    val strictBuilder = Definition.DefinitionBuilder.baseBuilder().`with`(d => d.mode = AnalysisMode.STRICT)

    "analyse if sepcfication includes only one module and no rules" in {
      val definition = strictBuilder.`with`(d => d.modules = List(MAIN).asJava).build()
      val actual = testee(orgExamples, None).analyse(Seq(definition)).get.head

      actual.evidenceBackedViolations.size shouldEqual 0
      actual.moduleConnectionViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(metric(MAIN.name, 0, 0))
    }

    "fail if no module is provided" in {
      val definition = strictBuilder.build()
      testee(orgExamples, None).analyse(Seq(definition)) should matchPattern {
        case Failure(_: AnalyserException) =>
      }
    }

    "analyse specification with many modules and rules" in {
      val definition = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER)).asJava
        d.noStrictDependencies = List(noSD(CONTROLLER, MAIN)).asJava
      }).build()

      val actual = testee(orgExamples, None).analyse(Seq(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs Seq(
        metric(MAIN.name, 0, 1),
        metric(CONTROLLER.name, 1, 0)
      )
    }

    "detect violations on specification" in {
      val definition = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)).asJava
        d.noStrictDependencies = List(noSD(MAIN, FACADE)).asJava
      }).build()

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
      val definition = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
      }).build()

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations should contain allElementsOf List(
        evidence(MAIN.name, CONTROLLER.name, List(), List(CONTROLLER.name), List(List(
          ("org.example.Main:main", "org.example.controller.Controller1:access"),
          ("org.example.Main:main", "org.example.controller.Controller1"),
          ("org.example.Main:main", "org.example.controller.Controller1:(init)")
        ))),
        evidence(MAIN.name, FACADE.name, List(), List(FACADE.name), List(List(
          ("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
          ("org.example.Main:main", "org.example.core.CoreFacade")
        ))),
        evidence(CONTROLLER.name, FACADE.name, List(), List(FACADE.name), List(List(
          ("org.example.controller.Controller1:access", "org.example.core.CoreFacade:facadeMethod1"),
          ("org.example.controller.Controller1", "org.example.core.CoreFacade"),
          ("org.example.controller.Controller1:(init)", "org.example.core.CoreFacade")
        )))
      )
    }

    "limit evidence for dependency violations if evidence limit configured" in {
      val definition = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
      }).build()

      val actual = testee(orgExamples, Some(1)).analyse(List(definition)).get.head

      val mainControllersEvidence = actual.evidenceBackedViolations
        .filter(v => v.sourceModule == MAIN.name && v.destinationModule == CONTROLLER.name)
        .head
        .evidences
      List(("org.example.Main:main", "org.example.controller.Controller1:access"),
        ("org.example.Main:main", "org.example.controller.Controller1"),
        ("org.example.Main:main", "org.example.controller.Controller1:(init)")) should contain allElementsOf mainControllersEvidence.head
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
      val definition = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI), dep(MAIN, IO),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
          dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS)
        ).asJava
        d.noStrictDependencies = List(
          noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS),
          noSD(IO, COREINTERNALS)
        ).asJava
      }).build()

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
      val definition = strictBuilder.`with`(d => {
        d.whitelist = AnonymousModule.make("org.example.Main", "org.example.controller.*")
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER)).asJava
      }).build()

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
      val definition = strictBuilder.`with`(d => {
        d.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*")
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, COREAPI),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI),
          dep(IO, MODEL)).asJava
        d.noStrictDependencies = List(noSD(CONTROLLER, COREINTERNALS), noSD(IO, COREINTERNALS)).asJava
      }).build()

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
      val definition = strictBuilder.`with`(d => {
        d.whitelist = AnonymousModule.make("org.example.*")
        d.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*")
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL)).asJava
        d.noStrictDependencies = List(noSD(CONTROLLER, COREINTERNALS), noSD(IO, COREINTERNALS)).asJava
      }).build()

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
      val definition1 = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER),
          dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)).asJava
        d.noStrictDependencies = List(noSD(MAIN, FACADE)).asJava
      }).build()
      val definition2 = strictBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI), dep(MAIN, IO),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
          dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS)).asJava
        d.noStrictDependencies = List(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS), noSD(IO, COREINTERNALS)).asJava
      }).build()

      val (actual1, actual2) = testee(orgExamples, None).analyse(List(definition1, definition2)).get match {
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

    val looseBuilder = Definition.DefinitionBuilder.baseBuilder()
      .`with`(d => d.mode = AnalysisMode.LOOSE)

    "analyse if specification has one module and no rule" in {
      val definition = looseBuilder.`with`(d => d.modules = List(MAIN).asJava).build()

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(metric(MAIN.name, 0, 0))
    }

    "fail if no module is provided" in {
      val definition = looseBuilder.build()
      testee(orgExamples, None).analyse(Seq(definition)) should matchPattern {
        case Failure(_: AnalyserException) =>
      }
    }

    "analyse specification with many modules and rules" in {
      val definition = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER)).asJava
        d.noStrictDependencies = List(noSD(CONTROLLER, MAIN)).asJava
      }).build()

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations.size shouldEqual 0
      actual.evidenceBackedViolations.size shouldEqual 0
      actual.metrics should contain theSameElementsAs List(
        metric("Main", 0, 1),
        metric("Controllers", 1, 0)
      )
    }

    "detect violations on the specification" in {
      val definition = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)).asJava
        d.noStrictDependencies = List(noSD(MAIN, FACADE)).asJava
      }).build()

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.moduleConnectionViolations should contain allElementsOf List(
        violation(CONTROLLER.name, MAIN.name)
      )
      actual.evidenceBackedViolations should contain allElementsOf List(
        evidence(MAIN.name, FACADE.name, List(MAIN.name, FACADE.name), List(FACADE.name), List(List(
          ("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
          ("org.example.Main:main", "org.example.core.CoreFacade")
        )))
      )
      actual.metrics should contain theSameElementsAs List(
        metric("Main", 0, 2),
        metric("Controllers", 1, 1),
        metric("Facade", 2, 0)
      )
    }

    "provide evidence for undesired dependencies" in {
      val definition = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
        d.noStrictDependencies = List(noSD(MAIN, CONTROLLER)).asJava
      }).build()

      val actual = testee(orgExamples, None).analyse(List(definition)).get.head

      actual.evidenceBackedViolations should contain allElementsOf List(
        evidence(MAIN.name, CONTROLLER.name, List(MAIN.name, CONTROLLER.name), List(CONTROLLER.name), List(List(
          ("org.example.Main:main", "org.example.controller.Controller1:access"),
          ("org.example.Main:main", "org.example.controller.Controller1"),
          ("org.example.Main:main", "org.example.controller.Controller1:(init)")
        )))
      )
    }

    "limit evidence provided if limit configured" in {
      val definition = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
        d.noStrictDependencies = List(noSD(MAIN, CONTROLLER)).asJava
      }).build()

      val actual = testee(orgExamples, Some(1)).analyse(List(definition)).get.head
      val mainController = actual.evidenceBackedViolations
        .filter(v => v.sourceModule == MAIN.name && v.destinationModule == CONTROLLER.name)
        .head
      List(("org.example.Main:main", "org.example.controller.Controller1:access"),
        ("org.example.Main:main", "org.example.controller.Controller1"),
        ("org.example.Main:main", "org.example.controller.Controller1:(init)")) should contain allElementsOf List(mainController.evidences.head.head)
      mainController.evidences.head.size shouldEqual 1
    }

    "analyse all modules in scope" in {
      val definition = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(MAIN, CONTROLLER), dep(MAIN, IO), dep(MAIN, MODEL),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS),
          dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL)).asJava
        d.noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN)).asJava
      }).build()

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
      val definition = looseBuilder.`with`(d => {
        d.whitelist = AnonymousModule.make("org.example.controller.*", "org.example.core.CoreFacade", "org.example.core.model.*")
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(dep(CONTROLLER, FACADE), dep(FACADE, MODEL)).asJava
        d.noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN)).asJava
      }).build()

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
      val definition = looseBuilder.`with`(d => {
        d.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*")
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL)).asJava
        d.noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN)).asJava
      }).build()

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
      val definition = looseBuilder.`with`(d => {
        d.whitelist = AnonymousModule.make("org.example.*")
        d.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*")
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL)
        ).asJava
      }).build()

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
      val definition1 = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE).asJava
        d.dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)).asJava
        d.noStrictDependencies = List(noSD(MAIN, FACADE)).asJava
      }).build()
      val definition2 = looseBuilder.`with`(d => {
        d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
        d.dependencies = List(
          dep(MAIN, CONTROLLER), dep(MAIN, IO),
          dep(MAIN, MODEL),
          dep(CONTROLLER, FACADE),
          dep(COREINTERNALS, MODEL),
          dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
          dep(COREAPI, MODEL),
          dep(IO, COREAPI), dep(IO, MODEL)).asJava
        d.noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN)).asJava
      }).build()

      val (actual1, actual2) = testee(orgExamples, None).analyse(List(definition1, definition2)).get match {
        case Seq(a1, a2) => (a1, a2)
      }

      verify(classParser).parse(refEq(orgExamples), any)

      actual1.moduleConnectionViolations should contain allElementsOf List(
        violation(CONTROLLER.name, MAIN.name)
      )
      actual1.evidenceBackedViolations should contain allElementsOf List(
        evidence(MAIN.name, FACADE.name, List(MAIN.name, FACADE.name), List(FACADE.name), List(List(
          ("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
          ("org.example.Main:main", "org.example.core.CoreFacade")
        )))
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
    val definition1 = Definition.DefinitionBuilder.baseBuilder().`with`(d => {
      d.mode = AnalysisMode.STRICT
      d.modules = List(MAIN, CONTROLLER, FACADE).asJava
      d.dependencies = List(dep(MAIN, CONTROLLER),
        dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)).asJava
      d.noStrictDependencies = List(noSD(MAIN, FACADE)).asJava
    }).build()
    val definition2 = Definition.DefinitionBuilder.baseBuilder().`with`(d => {
      d.mode = AnalysisMode.STRICT
      d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
      d.dependencies = List(
        dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI), dep(MAIN, IO),
        dep(CONTROLLER, FACADE),
        dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
        dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL),
        dep(COREAPI, MODEL),
        dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS)).asJava
      d.noStrictDependencies = List(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS), noSD(IO, COREINTERNALS)).asJava
    }).build()
    val definition3 = Definition.DefinitionBuilder.baseBuilder().`with`(d => {
      d.mode = AnalysisMode.LOOSE
      d.modules = List(MAIN, CONTROLLER, FACADE).asJava
      d.dependencies = List(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)).asJava
      d.noStrictDependencies = List(noSD(MAIN, FACADE)).asJava
    }).build()
    val definition4 = Definition.DefinitionBuilder.baseBuilder().`with`(d => {
      d.mode = AnalysisMode.LOOSE
      d.modules = List(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL).asJava
      d.dependencies = List(
        dep(MAIN, CONTROLLER), dep(MAIN, IO),
        dep(MAIN, MODEL),
        dep(CONTROLLER, FACADE),
        dep(COREINTERNALS, MODEL),
        dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
        dep(COREAPI, MODEL),
        dep(IO, COREAPI), dep(IO, MODEL)).asJava
      d.noStrictDependencies = List(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN)).asJava
    }).build()

    val (actual1, actual2, actual3, actual4) = testee(orgExamples, None).analyse(List(definition1, definition2, definition3, definition4)).get match {
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
      evidence(MAIN.name, FACADE.name, List(MAIN.name, FACADE.name), List(FACADE.name), List(List(
        ("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
        ("org.example.Main:main", "org.example.core.CoreFacade")
      )))
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
