package com.github.fburato.highwheelmodules.core

import com.github.fburato.highwheelmodules.utils.Pair

import java.util
import java.util.Optional

trait AnalyserFacade {
  def runAnalysis(classPathRoots: util.List[String], specificationPath: util.List[String], evidenceLimit: Optional[Integer]): Unit
}

trait Printer {
  def info(msg: String): Unit
}

trait PathEventSink {
  def ignoredPaths(ignored: util.List[String]): Unit

  def directories(directories: util.List[String]): Unit

  def jars(jars: util.List[String]): Unit
}

trait MeasureEventSink {
  def fanInOutMeasure(module: String, fanIn: Int, fanOut: Int): Unit
}

trait StrictAnalysisEventSink {
  def dependenciesCorrect(): Unit

  def directDependenciesCorrect(): Unit

  def dependencyViolationsPresent(): Unit

  def dependencyViolation(sourceModule: String, destModule: String, expectedPath: util.List[String], actualPath: util.List[String], evidences: util.List[util.List[Pair[String, String]]]): Unit

  def noDirectDependenciesViolationPresent(): Unit

  def noDirectDependencyViolation(sourceModule: String, destModule: String): Unit
}

trait LooseAnalysisEventSink {
  def allDependenciesPresent(): Unit

  def noUndesiredDependencies(): Unit

  def absentDependencyViolationsPresent(): Unit

  def absentDependencyViolation(sourceModule: String, destModule: String): Unit

  def undesiredDependencyViolationsPresent(): Unit

  def undesiredDependencyViolation(sourceModule: String, destModule: String, path: util.List[String], evidences: util.List[util.List[Pair[String, String]]]): Unit
}