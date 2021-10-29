package com.github.fburato.highwheelmodules.core

import com.github.fburato.highwheelmodules.core.analysis.AnalyserException
import com.github.fburato.highwheelmodules.core.specification.{CompilerException, ParserException}
import com.github.fburato.highwheelmodules.utils.Pair
import org.mockito.ArgumentMatcher
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Paths
import java.util.{Optional, List => JList}
import scala.collection.mutable.ArrayBuffer
import com.github.fburato.highwheelmodules.utils.Conversions._

class AnalyserFacadeTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest {
  private val accumulator = new ArrayBuffer[String]()
  private val printer = spy(new AccumulatorPrinter(accumulator))
  private val pathEventSink = spy(new AccumulatorPathEventSink(accumulator))
  private val measureEventSink = spy(new AccumulatorMeasureEventSink(accumulator))
  private val strictAnalysisEventSink = spy(new AccumulatorStrictAnalysisEventSink(accumulator))
  private val looseAnalysisEventSink = spy(new AccumulatorLooseAnalysisEventSink(accumulator))
  private val testee = new AnalyserFacadeImpl(
    printer,
    pathEventSink,
    measureEventSink,
    strictAnalysisEventSink,
    looseAnalysisEventSink
  )

  private val defaultSpec = getPathToExistingFile("spec.hwm")

  private def getPathToExistingFile(file: String): String = {
    val alternatives = Seq("scala-2.12", "scala-2.13")
    alternatives
      .map(scalaVersion => Paths.get("core", "target", scalaVersion, "test-classes", file))
      .find(p => p.toFile.exists())
      .map(_.toString)
      .get
  }

  private val defaultSpecWhiteBlack = getPathToExistingFile("spec-whiteblack.hwm")
  private val wrongSpecWhiteBlack = getPathToExistingFile("wrong-spec-whiteblack.hwm")
  private val alternativeStrictSpec = getPathToExistingFile("alternate-strict-spec.hwm")
  private val jarPath = getPathToExistingFile("highwheel-model.jar")
  private val wrongSpec = getPathToExistingFile("wrong-syntax-spec.hwm")
  private val wrongSemanticsSpec = getPathToExistingFile("wrong-semantics-spec.hwm")
  private val wrongStrictDefinitionSpec = getPathToExistingFile("wrong-strict-spec.hwm")
  private val looseSpec = getPathToExistingFile("loose-spec.hwm")
  private val looseSpecWhiteBlack = getPathToExistingFile("loose-spec-whiteblack.hwm")
  private val wrongLooseSpecWhiteBlack = getPathToExistingFile("wrong-loose-spec-whiteblack.hwm")
  private val orgExamplePath = getPathToExistingFile("org")
  private val wrongLooseDefinitionSpec = getPathToExistingFile("wrong-loose-spec.hwm")

  def list[T](args: T*): JList[T] = args.asJava

  def anyMatches(regex: String): ArgumentMatcher[JList[String]] =
    (arguments: JList[String]) => arguments.asScala.exists(e => e.matches(regex))

  def anyContains(regex: String): JList[String] = argThat(anyMatches(regex))

  "analysis" should {

    "print as info jars passed as arguments" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(jarPath), list(defaultSpec), Optional.empty())
      }
      verify(pathEventSink).jars(anyContains(".*highwheel-model\\.jar.*"))
    }

    "print as info directories passed as argument" in {
      testee.runAnalysis(list(orgExamplePath), list(defaultSpec), Optional.empty())

      verify(pathEventSink).directories(anyContains(".*test-classes.*org.*"))
    }

    "print ignored files that do not exist as info" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list("foobar"), list(defaultSpec), Optional.empty())
      }
      verify(pathEventSink).ignoredPaths(anyContains(".*foobar.*"))
    }

    "print ignored directories and jars as info" in {
      testee.runAnalysis(
        list(jarPath, orgExamplePath, "foobar"),
        list(defaultSpec),
        Optional.empty()
      )

      verify(pathEventSink).jars(anyContains(".*highwheel-model\\.jar.*"))
      verify(pathEventSink).directories(anyContains(".*test-classes.*org.*"))
      verify(pathEventSink).ignoredPaths(anyContains(".*foobar.*"))
    }

    "fail if specification does not exist" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list("foobar"), Optional.empty())
      }
    }

    "fail on parse failure" in {
      assertThrows[CompilerException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongSemanticsSpec), Optional.empty())
      }
      verify(printer).info(matches(".*Compiling specification.*"))
    }

    "produce strict analysis output on strict specification" in {
      testee.runAnalysis(list(orgExamplePath), list(defaultSpec), Optional.empty())

      verify(strictAnalysisEventSink).dependenciesCorrect()
      verify(strictAnalysisEventSink).directDependenciesCorrect()
    }

    "produce strict analysis output with white blacklist specification" in {
      testee.runAnalysis(list(orgExamplePath), list(defaultSpecWhiteBlack), Optional.empty())

      verify(strictAnalysisEventSink).dependenciesCorrect()
      verify(strictAnalysisEventSink).directDependenciesCorrect()
    }

    "fail on strict analysis with white and blaclist" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongSpecWhiteBlack), Optional.empty())
      }

      verify(strictAnalysisEventSink).dependencyViolationsPresent()
      verify(strictAnalysisEventSink)
        .dependencyViolation("Main", "Facade", list("Main", "Controller", "Facade"), list(), list())
    }

    "produce loose output when no violation occurs" in {
      testee.runAnalysis(list(orgExamplePath), list(looseSpec), Optional.empty())

      verify(looseAnalysisEventSink).allDependenciesPresent()
      verify(looseAnalysisEventSink).noUndesiredDependencies()
    }

    "produce loose analysis output with white and blacklist" in {
      testee.runAnalysis(list(orgExamplePath), list(looseSpecWhiteBlack), Optional.empty())

      verify(looseAnalysisEventSink).allDependenciesPresent()
      verify(looseAnalysisEventSink).noUndesiredDependencies()
    }

    "fail on loose analysis output with white and blacklist" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongLooseSpecWhiteBlack), Optional.empty())
      }

      verify(looseAnalysisEventSink).absentDependencyViolationsPresent()
      verify(looseAnalysisEventSink).absentDependencyViolation("Main", "Controller")
    }

    def verifyStrictMetrics(): Unit = {
      verify(measureEventSink).fanInOutMeasure("Facade", 2, 3)
      verify(measureEventSink).fanInOutMeasure("Utils", 2, 0)
      verify(measureEventSink).fanInOutMeasure("IO", 1, 3)
      verify(measureEventSink).fanInOutMeasure("Model", 4, 0)
      verify(measureEventSink).fanInOutMeasure("CoreInternals", 1, 3)
      verify(measureEventSink).fanInOutMeasure("CoreApi", 4, 1)
      verify(measureEventSink).fanInOutMeasure("Controller", 1, 1)
      verify(measureEventSink).fanInOutMeasure("Main", 0, 4)
    }

    "produce metrics on strict analysis" in {
      testee.runAnalysis(list(orgExamplePath), list(defaultSpec), Optional.empty())

      verifyStrictMetrics()
    }

    def verifyLooseMetrics(): Unit = {
      verify(measureEventSink).fanInOutMeasure("Facade", 2, 3)
      verify(measureEventSink).fanInOutMeasure("Utils", 2, 0)
      verify(measureEventSink).fanInOutMeasure("IO", 1, 3)
      verify(measureEventSink).fanInOutMeasure("Model", 4, 0)
      verify(measureEventSink).fanInOutMeasure("CoreInternals", 1, 2)
      verify(measureEventSink).fanInOutMeasure("CoreApi", 3, 1)
      verify(measureEventSink).fanInOutMeasure("Controller", 1, 1)
      verify(measureEventSink).fanInOutMeasure("Main", 0, 4)
    }

    "produce metrics on loose analysis" in {
      testee.runAnalysis(list(orgExamplePath), list(looseSpec), Optional.empty())

      verifyLooseMetrics()
    }

    def pair(one: String, two: String): Pair[String, String] = Pair(one, two)

    "print violations on failed strict analysis" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongStrictDefinitionSpec), Optional.empty())
      }

      verify(strictAnalysisEventSink).dependencyViolationsPresent()
      verify(strictAnalysisEventSink).dependencyViolation(
        "IO",
        "Utils",
        list(),
        list("IO", "Utils"),
        list(
          list(
            pair("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util"),
            pair("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util1"),
            pair("org.example.io.IOImplementaion:something", "org.example.commons.Utility:util")
          )
        )
      )
      verify(strictAnalysisEventSink).noDirectDependenciesViolationPresent()
      verify(strictAnalysisEventSink).noDirectDependencyViolation("Facade", "CoreInternals")
    }

    "fail and limit the violations on strict analysis" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongStrictDefinitionSpec), Optional.of(1))
      }
      verify(strictAnalysisEventSink).dependencyViolationsPresent()
      verify(strictAnalysisEventSink).dependencyViolation(
        "IO",
        "Utils",
        list(),
        list("IO", "Utils"),
        list(
          list(pair("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util"))
        )
      )
      verify(strictAnalysisEventSink).noDirectDependenciesViolationPresent()
      verify(strictAnalysisEventSink).noDirectDependencyViolation("Facade", "CoreInternals")
    }

    "fail and print the violations on loose analysis" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongLooseDefinitionSpec), Optional.empty())
      }
      verify(looseAnalysisEventSink).absentDependencyViolationsPresent()
      verify(looseAnalysisEventSink).undesiredDependencyViolationsPresent()
      verify(looseAnalysisEventSink).absentDependencyViolation("IO", "CoreInternals")
      verify(looseAnalysisEventSink).undesiredDependencyViolation(
        "IO",
        "Model",
        list("IO", "Model"),
        list(
          list(
            pair("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1"),
            pair("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1:(init)")
          )
        )
      )
    }

    "fail and limit the violations on loose analysis" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(list(orgExamplePath), list(wrongLooseDefinitionSpec), Optional.of(1))
      }
      verify(looseAnalysisEventSink).absentDependencyViolationsPresent()
      verify(looseAnalysisEventSink).undesiredDependencyViolationsPresent()
      verify(looseAnalysisEventSink).absentDependencyViolation("IO", "CoreInternals")
      verify(looseAnalysisEventSink).undesiredDependencyViolation(
        "IO",
        "Model",
        list("IO", "Model"),
        list(list(pair("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1")))
      )
    }

    "fail if any of the specifications does not compile on strict" in {
      assertThrows[ParserException] {
        testee.runAnalysis(list(orgExamplePath), list(defaultSpec, wrongSpec), Optional.empty())
      }
      accumulator should contain theSameElementsInOrderAs List(
        "IGNORED_PATHS - ",
        "DIRECTORIES - 1",
        "JARS - ",
        s"INFO - Compiling specification '$defaultSpec'",
        "INFO - Done!",
        s"INFO - Compiling specification '$wrongSpec'"
      )
    }

    "analyse all specifications successfully on strict analysis" in {
      testee.runAnalysis(
        list(orgExamplePath),
        list(defaultSpec, alternativeStrictSpec),
        Optional.empty()
      )

      accumulator should contain theSameElementsAs List(
        "IGNORED_PATHS - ",
        "DIRECTORIES - 1",
        "JARS - ",
        s"INFO - Compiling specification '$defaultSpec'",
        "INFO - Done!",
        s"INFO - Compiling specification '$alternativeStrictSpec'",
        "INFO - Done!",
        s"INFO - Starting strict analysis on '$defaultSpec'",
        "FAN_IN_OUT_MEASURE - Facade,2,3",
        "FAN_IN_OUT_MEASURE - Utils,2,0",
        "FAN_IN_OUT_MEASURE - IO,1,3",
        "FAN_IN_OUT_MEASURE - Model,4,0",
        "FAN_IN_OUT_MEASURE - CoreInternals,1,3",
        "FAN_IN_OUT_MEASURE - CoreApi,4,1",
        "FAN_IN_OUT_MEASURE - Controller,1,1",
        "FAN_IN_OUT_MEASURE - Main,0,4",
        "DEPENDENCIES_CORRECT",
        "DIRECT_DEPENDENCIES_CORRECT",
        s"INFO - Analysis on '$defaultSpec' complete",
        s"INFO - Starting strict analysis on '$alternativeStrictSpec'",
        "FAN_IN_OUT_MEASURE - Internals,1,0",
        "FAN_IN_OUT_MEASURE - Main,0,1",
        "DEPENDENCIES_CORRECT",
        s"DIRECT_DEPENDENCIES_CORRECT",
        s"INFO - Analysis on '$alternativeStrictSpec' complete"
      )
    }

    "fail if one specification but complete successful anlyses on strict" in {
      assertThrows[AnalyserException] {
        testee.runAnalysis(
          list(orgExamplePath),
          list(wrongStrictDefinitionSpec, alternativeStrictSpec),
          Optional.empty()
        )
      }

      accumulator should contain theSameElementsAs List(
        "IGNORED_PATHS - ",
        "DIRECTORIES - 1",
        "JARS - ",
        s"INFO - Compiling specification '$wrongStrictDefinitionSpec'",
        "INFO - Done!",
        s"INFO - Compiling specification '$alternativeStrictSpec'",
        "INFO - Done!",
        s"INFO - Starting strict analysis on '$wrongStrictDefinitionSpec'",
        "FAN_IN_OUT_MEASURE - Facade,2,3",
        "FAN_IN_OUT_MEASURE - Utils,2,0",
        "FAN_IN_OUT_MEASURE - IO,1,3",
        "FAN_IN_OUT_MEASURE - Model,4,0",
        "FAN_IN_OUT_MEASURE - CoreInternals,1,3",
        "FAN_IN_OUT_MEASURE - CoreApi,4,1",
        "FAN_IN_OUT_MEASURE - Controller,1,1",
        "FAN_IN_OUT_MEASURE - Main,0,4",
        "DEPENDENCY_VIOLATION_PRESENT",
        "DEPENDENCY_VIOLATION - {IO,Utils,[],[IO,Utils],[1]}",
        "DEPENDENCY_VIOLATION - {Main,Utils,[Main,Facade,CoreInternals,Utils],[Main,IO,Utils],[2]}",
        "NO_DIRECT_DEPENDENCIES_VIOLATION_PRESENT",
        "NO_DIRECT_DEPENDENCY_VIOLATION - Facade,CoreInternals",
        s"INFO - Analysis on '$wrongStrictDefinitionSpec' failed",
        s"INFO - Starting strict analysis on '$alternativeStrictSpec'",
        "FAN_IN_OUT_MEASURE - Internals,1,0",
        "FAN_IN_OUT_MEASURE - Main,0,1",
        "DEPENDENCIES_CORRECT",
        "DIRECT_DEPENDENCIES_CORRECT",
        s"INFO - Analysis on '$alternativeStrictSpec' complete"
      )
    }

    "make alternate analyses successfully" in {
      testee.runAnalysis(
        list(orgExamplePath),
        list(looseSpec, alternativeStrictSpec),
        Optional.of(0)
      )

      accumulator should contain theSameElementsAs List(
        "IGNORED_PATHS - ",
        "DIRECTORIES - 1",
        "JARS - ",
        s"INFO - Compiling specification '$looseSpec'",
        "INFO - Done!",
        s"INFO - Compiling specification '$alternativeStrictSpec'",
        "INFO - Done!",
        s"INFO - Starting loose analysis on '$looseSpec'",
        "FAN_IN_OUT_MEASURE - Facade,2,3",
        "FAN_IN_OUT_MEASURE - Utils,2,0",
        "FAN_IN_OUT_MEASURE - IO,1,3",
        "FAN_IN_OUT_MEASURE - Model,4,0",
        "FAN_IN_OUT_MEASURE - CoreInternals,1,2",
        "FAN_IN_OUT_MEASURE - CoreApi,3,1",
        "FAN_IN_OUT_MEASURE - Controller,1,1",
        "FAN_IN_OUT_MEASURE - Main,0,4",
        "NO_UNDESIRED_DEPENDENCIES",
        "ALL_DEPENDENCIES_PRESENT",
        s"INFO - Analysis on '$looseSpec' complete",
        s"INFO - Starting strict analysis on '$alternativeStrictSpec'",
        "FAN_IN_OUT_MEASURE - Internals,1,0",
        "FAN_IN_OUT_MEASURE - Main,0,1",
        "DEPENDENCIES_CORRECT",
        "DIRECT_DEPENDENCIES_CORRECT",
        s"INFO - Analysis on '$alternativeStrictSpec' complete"
      )
    }
  }
}
