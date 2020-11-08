package com.github.fburato.highwheelmodules

import java.util

import com.github.fburato.highwheelmodules.utils.Pair

import scala.collection.mutable.ArrayBuffer

package object core {

  class AccumulatorPrinter(private val accumulator: ArrayBuffer[String]) extends AnalyserFacade.Printer {
    override def info(msg: String): Unit =
      accumulator addOne s"INFO - $msg"
  }

  class AccumulatorPathEventSink(private val accumulator: ArrayBuffer[String]) extends AnalyserFacade.EventSink.PathEventSink {
    override def ignoredPaths(ignored: util.List[String]): Unit =
      accumulator addOne s"IGNORED_PATHS - ${String.join(",", ignored)}"

    override def directories(directories: util.List[String]): Unit =
      accumulator addOne s"DIRECTORIES - ${directories.size()}"

    override def jars(jars: util.List[String]): Unit =
      accumulator addOne s"JARS - ${String.join(",", jars)}"
  }

  class AccumulatorMeasureEventSink(private val accumulator: ArrayBuffer[String]) extends AnalyserFacade.EventSink.MeasureEventSink {
    override def fanInOutMeasure(module: String, fanIn: Int, fanOut: Int): Unit =
      accumulator addOne s"FAN_IN_OUT_MEASURE - $module,$fanIn,$fanOut"
  }

  class AccumulatorStrictAnalysisEventSink(private val accumulator: ArrayBuffer[String]) extends AnalyserFacade.EventSink.StrictAnalysisEventSink {
    override def dependenciesCorrect(): Unit =
      accumulator addOne "DEPENDENCIES_CORRECT"

    override def directDependenciesCorrect(): Unit =
      accumulator addOne "DIRECT_DEPENDENCIES_CORRECT"

    override def dependencyViolationsPresent(): Unit =
      accumulator addOne "DEPENDENCY_VIOLATION_PRESENT"

    override def dependencyViolation(sourceModule: String,
                                     destModule: String,
                                     expectedPath: util.List[String],
                                     actualPath: util.List[String],
                                     evidences: util.List[util.List[Pair[String, String]]]): Unit =
      accumulator addOne s"DEPENDENCY_VIOLATION - {$sourceModule,$destModule,[${String.join(",", expectedPath)}],[${String.join(",", actualPath)}],[${evidences.size()}]}"

    override def noDirectDependenciesViolationPresent(): Unit =
      accumulator addOne "NO_DIRECT_DEPENDENCIES_VIOLATION_PRESENT"

    override def noDirectDependencyViolation(sourceModule: String, destModule: String): Unit =
      accumulator addOne s"NO_DIRECT_DEPENDENCY_VIOLATION - $sourceModule,$destModule"
  }

  class AccumulatorLooseAnalysisEventSink(private val accumulator: ArrayBuffer[String]) extends AnalyserFacade.EventSink.LooseAnalysisEventSink {
    override def allDependenciesPresent(): Unit =
      accumulator addOne "ALL_DEPENDENCIES_PRESENT"

    override def noUndesiredDependencies(): Unit =
      accumulator addOne "NO_UNDESIRED_DEPENDENCIES"

    override def absentDependencyViolationsPresent(): Unit =
      accumulator addOne "ABSENT_DEPENDENCY_VIOLATIONS_PRESENT"

    override def absentDependencyViolation(sourceModule: String, destModule: String): Unit =
      accumulator addOne s"ABSENT_DEPENDENCY_VIOLATION - $sourceModule,$destModule"

    override def undesiredDependencyViolationsPresent(): Unit =
      accumulator addOne "UNDESIRED_DEPENDENCY_VIOLATION_PRESENT"

    override def undesiredDependencyViolation(sourceModule: String,
                                              destModule: String,
                                              path: util.List[String],
                                              evidences: util.List[util.List[Pair[String, String]]]): Unit =
      accumulator addOne s"UNDESIRED_DEPENDENCY_VIOLATION - {$sourceModule,$destModule,[${String.join(",", path)}],[${evidences.size()}]}"
  }

}
