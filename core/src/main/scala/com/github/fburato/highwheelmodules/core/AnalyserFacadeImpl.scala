package com.github.fburato.highwheelmodules.core

import com.github.fburato.highwheelmodules.bytecodeparser.ClassPathParserS
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.{ArchiveClassPathRootS, CompoundClassPathRootS, DirectoryClassPathRootS}
import com.github.fburato.highwheelmodules.core.AnalyserFacade.EventSink.{LooseAnalysisEventSink, MeasureEventSink, PathEventSink, StrictAnalysisEventSink}
import com.github.fburato.highwheelmodules.core.AnalyserFacade.Printer
import com.github.fburato.highwheelmodules.core.analysis._
import com.github.fburato.highwheelmodules.core.externaladapters.GuavaGraphFactory
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode
import com.github.fburato.highwheelmodules.model.classpath.{ClassParser, ClasspathRoot}
import com.github.fburato.highwheelmodules.model.modules.{Definition, ModuleGraphFactory}
import com.github.fburato.highwheelmodules.utils.Pair

import java.io.File
import java.util.{List => JList, Optional => JOptional}
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.{Failure, Success, Try}

class AnalyserFacadeImpl private[core](printer: Printer,
                                       pathEventSink: PathEventSink,
                                       measureEventSink: MeasureEventSink,
                                       strictAnalysisEventSink: StrictAnalysisEventSink,
                                       looseAnalysisEventSink: LooseAnalysisEventSink,
                                       classParser: ClassParser,
                                       moduleGraphFactory: ModuleGraphFactory,
                                       specificationCompiler: SpecificationCompiler) extends AnalyserFacade {

  def this(printer: Printer,
           pathEventSink: PathEventSink,
           measureEventSink: MeasureEventSink,
           strictAnalysisEventSink: StrictAnalysisEventSink,
           looseAnalysisEventSink: LooseAnalysisEventSink) = {
    this(printer, pathEventSink, measureEventSink, strictAnalysisEventSink, looseAnalysisEventSink,
      new ClassPathParserS(_ => true),
      new GuavaGraphFactory,
      SpecificationCompiler()
    )
  }

  import AnalyserFacadeImpl._

  private val strictEventSink = new DelegateStrictAnalysisEventSink(strictAnalysisEventSink)
  private val looseEventSink = new DelegateLooseAnalysisEventSink(looseAnalysisEventSink)

  private def getEventSink(definition: Definition): AnalysisEventSink = definition.mode match {
    case AnalysisMode.LOOSE => looseEventSink
    case AnalysisMode.STRICT => strictEventSink
  }

  override def runAnalysis(classPathRoots: JList[String], specificationPath: JList[String], evidenceLimit: JOptional[Integer]): Unit = {
    val classPathRoot = getAnalysisScope(classPathRoots)
    val definitionsAndPaths = specificationPath.asScala.toSeq.map { path => compileSpecification(path).map(d => (path, d)) }
    val traversed = sequence(definitionsAndPaths)
    val analyser = ModuleAnalyser(classParser, classPathRoot, evidenceLimit.toScala.map(i => i.toInt), moduleGraphFactory)

    def analyseAndCollect(pathDefinitions: Seq[(String, Definition)]): Try[Seq[(String, Definition, AnalysisResult)]] = {
      val definitions = pathDefinitions.map(_._2)
      val analysisResults = analyser.analyse(definitions)
      analysisResults.map(results => (pathDefinitions zip results) map {
        case ((path, definition), result) => (path, definition, result)
      })
    }

    val results = for {
      seq <- traversed
      analysed <- analyseAndCollect(seq)
    } yield analysed map {
      case (path, definition, analysisResult) => analysis(path, analysisResult, getEventSink(definition))
    } forall (s => !s)

    results.flatMap {
      case true => Success(())
      case false => Failure(AnalyserException("Analysis failed"))
    }.get
  }

  private def getAnalysisScope(paths: JList[String]): ClasspathRoot = {
    case class Classification(jars: ArrayBuffer[File], dirs: ArrayBuffer[File], ignored: ArrayBuffer[String])
    def classify(paths: JList[String]): Classification = {
      val jars = new ArrayBuffer[File]
      val dirs = new ArrayBuffer[File]
      val ignored = new ArrayBuffer[String]
      paths.forEach { path =>
        val file = new File(path)
        if (!file.exists || !file.canRead || (file.isFile && !path.endsWith(".jar"))) {
          ignored += path
        } else if (file.isDirectory) {
          dirs += file
        } else {
          jars += file
        }
      }
      Classification(jars, dirs, ignored)
    }

    val classification = classify(paths)
    pathEventSink ignoredPaths classification.ignored.asJava
    pathEventSink directories classification.dirs.map(_.getAbsolutePath).asJava
    pathEventSink jars classification.jars.map(_.getAbsolutePath).asJava

    new CompoundClassPathRootS(
      (classification.dirs.map(f => new DirectoryClassPathRootS(f).asInstanceOf[ClasspathRoot]) ++
        classification.jars.map(f => new ArchiveClassPathRootS(f).asInstanceOf[ClasspathRoot])).asJava)
  }

  private def compileSpecification(specificationPath: String): Try[Definition] = {
    val specificationFile = new File(specificationPath)
    if (!specificationFile.exists || specificationFile.isDirectory || !specificationFile.canRead) {
      Failure(AnalyserException(s"Cannot read from specification file '$specificationPath'"))
    } else {
      printer.info(s"Compiling specification '$specificationPath'")
      specificationCompiler.compile(specificationFile).map(d => {
        printer.info("Done!")
        d
      })
    }
  }

  private def analysis(path: String, analysisResult: AnalysisResult, eventSink: AnalysisEventSink): Boolean = {
    printer.info(s"Starting ${eventSink.analysisType} analysis on '$path'")
    printMetrics(analysisResult.metrics)
    if (analysisResult.evidenceBackedViolations.isEmpty) {
      eventSink.dependentDefinitionsCorrect()
    } else {
      eventSink.dependentViolationsPresent()
      val violations = analysisResult.evidenceBackedViolations
      violations.sortBy(v => (v.sourceModule, v.destinationModule)).foreach(v => eventSink.signalDependentViolation(v.sourceModule, v.destinationModule,
        appendStartIfNotEmpty(v.specificationPath, v.sourceModule),
        appendStartIfNotEmpty(v.actualPath, v.sourceModule), v.evidences))
    }
    if (analysisResult.moduleConnectionViolations.isEmpty) {
      eventSink.nonDependentDefinitionsCorrect()
    } else {
      eventSink.nonDependentViolationsPresent()
      val violations = analysisResult.moduleConnectionViolations
      violations.sortBy(v => (v.sourceModule, v.destinationModule)).foreach(v => eventSink.signalNonDependentViolation(v.sourceModule, v.destinationModule))
    }
    if (analysisResult.evidenceBackedViolations.nonEmpty || analysisResult.moduleConnectionViolations.nonEmpty) {
      printer.info(s"Analysis on '$path' failed")
      true
    } else {
      printer.info(s"Analysis on '$path' complete")
      false
    }
  }

  private def printMetrics(metrics: Seq[Metric]): Unit = {
    metrics.sortBy(_.module).foreach(m => measureEventSink.fanInOutMeasure(m.module, m.fanIn, m.fanOut))
  }

  private def appendStartIfNotEmpty[T](collection: Seq[T], start: T): Seq[T] =
    if (collection.isEmpty) {
      collection
    } else {
      Seq(start) ++ collection
    }
}

private[core] object AnalyserFacadeImpl {

  private trait AnalysisEventSink {

    def analysisType: String

    def dependentDefinitionsCorrect(): Unit

    def nonDependentDefinitionsCorrect(): Unit

    def dependentViolationsPresent(): Unit

    def nonDependentViolationsPresent(): Unit

    def signalDependentViolation(sourceModule: String, destinationModule: String, expectedPath: Seq[String], actualPath: Seq[String], evidences: Seq[Seq[(String, String)]]): Unit

    def signalNonDependentViolation(sourceModule: String, destinationModule: String): Unit
  }

  private class DelegateStrictAnalysisEventSink(private val delegate: StrictAnalysisEventSink) extends AnalysisEventSink {
    override def analysisType: String = "strict"

    override def dependentDefinitionsCorrect(): Unit = delegate.dependenciesCorrect()

    override def nonDependentDefinitionsCorrect(): Unit = delegate.directDependenciesCorrect()

    override def dependentViolationsPresent(): Unit = delegate.dependencyViolationsPresent()

    override def nonDependentViolationsPresent(): Unit = delegate.noDirectDependenciesViolationPresent()

    override def signalDependentViolation(sourceModule: String, destinationModule: String, expectedPath: Seq[String], actualPath: Seq[String], evidences: Seq[Seq[(String, String)]]): Unit =
      delegate.dependencyViolation(sourceModule, destinationModule, expectedPath.asJava, actualPath.asJava, convertEvidences(evidences))

    override def signalNonDependentViolation(sourceModule: String, destinationModule: String): Unit =
      delegate.noDirectDependencyViolation(sourceModule, destinationModule)
  }

  private def convertEvidences(evidences: Seq[Seq[(String, String)]]): JList[JList[Pair[String, String]]] = evidences.map(e => e.map({
    case (p1, p2) => Pair.make(p1, p2)
  }).asJava).asJava

  private class DelegateLooseAnalysisEventSink(private val delegate: LooseAnalysisEventSink) extends AnalysisEventSink {

    override def analysisType: String = "loose"

    override def dependentDefinitionsCorrect(): Unit = delegate.noUndesiredDependencies()

    override def nonDependentDefinitionsCorrect(): Unit = delegate.allDependenciesPresent()

    override def dependentViolationsPresent(): Unit = delegate.undesiredDependencyViolationsPresent()

    override def nonDependentViolationsPresent(): Unit = delegate.absentDependencyViolationsPresent()

    override def signalDependentViolation(sourceModule: String, destinationModule: String, expectedPath: Seq[String], actualPath: Seq[String], evidences: Seq[Seq[(String, String)]]): Unit =
      delegate.undesiredDependencyViolation(sourceModule, destinationModule, actualPath.asJava, convertEvidences(evidences))

    override def signalNonDependentViolation(sourceModule: String, destinationModule: String): Unit =
      delegate.absentDependencyViolation(sourceModule, destinationModule)
  }

  private def sequence[T](xs: Seq[Try[T]]): Try[Seq[T]] = xs.foldLeft(Try(Seq[T]())) {
    (a, b) => a flatMap (c => b map (d => c :+ d))
  }
}