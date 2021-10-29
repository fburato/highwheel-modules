package com.github.fburato.highwheelmodules

import com.github.fburato.highwheelmodules.utils.Pair

import java.util
import scala.collection.mutable.ArrayBuffer

package object core {

  class AccumulatorPrinter(private val accumulator: ArrayBuffer[String]) extends Printer {
    override def info(msg: String): Unit =
      accumulator append s"INFO - $msg"
  }

  class AccumulatorPathEventSink(private val accumulator: ArrayBuffer[String])
      extends PathEventSink {
    override def ignoredPaths(ignored: util.List[String]): Unit =
      accumulator append s"IGNORED_PATHS - ${String.join(",", ignored)}"

    override def directories(directories: util.List[String]): Unit =
      accumulator append s"DIRECTORIES - ${directories.size()}"

    override def jars(jars: util.List[String]): Unit =
      accumulator append s"JARS - ${String.join(",", jars)}"
  }

  class AccumulatorMeasureEventSink(private val accumulator: ArrayBuffer[String])
      extends MeasureEventSink {
    override def fanInOutMeasure(module: String, fanIn: Int, fanOut: Int): Unit =
      accumulator append s"FAN_IN_OUT_MEASURE - $module,$fanIn,$fanOut"
  }

  class AccumulatorStrictAnalysisEventSink(private val accumulator: ArrayBuffer[String])
      extends StrictAnalysisEventSink {
    override def dependenciesCorrect(): Unit =
      accumulator append "DEPENDENCIES_CORRECT"

    override def directDependenciesCorrect(): Unit =
      accumulator append "DIRECT_DEPENDENCIES_CORRECT"

    override def dependencyViolationsPresent(): Unit =
      accumulator append "DEPENDENCY_VIOLATION_PRESENT"

    override def dependencyViolation(
      sourceModule: String,
      destModule: String,
      expectedPath: util.List[String],
      actualPath: util.List[String],
      evidences: util.List[util.List[Pair[String, String]]]
    ): Unit =
      accumulator append s"DEPENDENCY_VIOLATION - {$sourceModule,$destModule,[${String.join(",", expectedPath)}],[${String
        .join(",", actualPath)}],[${evidences.size()}]}"

    override def noDirectDependenciesViolationPresent(): Unit =
      accumulator append "NO_DIRECT_DEPENDENCIES_VIOLATION_PRESENT"

    override def noDirectDependencyViolation(sourceModule: String, destModule: String): Unit =
      accumulator append s"NO_DIRECT_DEPENDENCY_VIOLATION - $sourceModule,$destModule"
  }

  class AccumulatorLooseAnalysisEventSink(private val accumulator: ArrayBuffer[String])
      extends LooseAnalysisEventSink {
    override def allDependenciesPresent(): Unit =
      accumulator append "ALL_DEPENDENCIES_PRESENT"

    override def noUndesiredDependencies(): Unit =
      accumulator append "NO_UNDESIRED_DEPENDENCIES"

    override def absentDependencyViolationsPresent(): Unit =
      accumulator append "ABSENT_DEPENDENCY_VIOLATIONS_PRESENT"

    override def absentDependencyViolation(sourceModule: String, destModule: String): Unit =
      accumulator append s"ABSENT_DEPENDENCY_VIOLATION - $sourceModule,$destModule"

    override def undesiredDependencyViolationsPresent(): Unit =
      accumulator append "UNDESIRED_DEPENDENCY_VIOLATION_PRESENT"

    override def undesiredDependencyViolation(
      sourceModule: String,
      destModule: String,
      path: util.List[String],
      evidences: util.List[util.List[Pair[String, String]]]
    ): Unit =
      accumulator append s"UNDESIRED_DEPENDENCY_VIOLATION - {$sourceModule,$destModule,[${String
        .join(",", path)}],[${evidences.size()}]}"
  }

}
