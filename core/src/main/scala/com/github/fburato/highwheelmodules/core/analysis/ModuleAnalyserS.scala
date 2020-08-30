package com.github.fburato.highwheelmodules.core.analysis

import java.io.IOException
import java.util.{Optional, List => JList}

import com.github.fburato.highwheelmodules.core.algorithms.CompoundAccessVisitor
import com.github.fburato.highwheelmodules.core.analysis.AnalyserModel.AnalysisResult
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode
import com.github.fburato.highwheelmodules.model.classpath.{AccessVisitor, ClassParser, ClasspathRoot}
import com.github.fburato.highwheelmodules.model.modules.{Definition, ModuleGraphFactory}

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.{Failure, Success, Try}

trait ModuleAnalyserS {
  def analyse(definitions: JList[Definition]): JList[AnalysisResult] = analyse(definitions.asScala.toSeq).get.asJava

  def analyse(definitions: Seq[Definition]): Try[Seq[AnalysisResult]]
}

object ModuleAnalyserS {

  private class Implementation(classParser: ClassParser, classpathRoot: ClasspathRoot, evidenceLimit: Option[Int], factory: ModuleGraphFactory) extends ModuleAnalyserS {
    private val definitionVisitor = new DefinitionVisitor(factory, evidenceLimit.map(i => new Integer(i)).toJava)
    private val strictAnalyser = new StrictAnalyser()
    private val looseAnalyser = new LooseAnalyser()

    override def analyse(definitions: Seq[Definition]): Try[Seq[AnalysisResult]] =
      if(definitions.isEmpty) {
        Success(Seq())
      } else {
        for {
          visitorsAndProcessors <- visitorAndProcessors(definitions)
          (visitor, processors) = visitorsAndProcessors
          _ <- Try(classParser.parse(classpathRoot, visitor)) recoverWith {
            case e: IOException => Failure(new AnalyserException(e))
          }
          results <- sequence(processors.map(p => Try(p())))
        } yield results
      }

    private def visitorAndProcessors(definitions: Seq[Definition]): Try[(AccessVisitor, Seq[() => AnalysisResult])] = {
      val merged = definitions.map(d => Try {
        val state = definitionVisitor.getAnalysisState(d)
        (state.visitor, () => d.mode match {
          case AnalysisMode.STRICT => strictAnalyser.analyse(state)
          case AnalysisMode.LOOSE => looseAnalyser.analyse(state)
        })
      })
      sequence(merged).map(sequence => {
        val (visitors, processors) = sequence.unzip
        (new CompoundAccessVisitor(visitors.asJava), processors)
      })
    }
  }

  def apply(classParser: ClassParser, classpathRoot: ClasspathRoot, evidenceLimit: Optional[Integer], factory: ModuleGraphFactory): ModuleAnalyserS =
    new Implementation(classParser, classpathRoot, evidenceLimit.map(i => i.asInstanceOf[Int]).toScala, factory)

  def apply(classParser: ClassParser, classpathRoot: ClasspathRoot, evidenceLimit: Option[Int], factory: ModuleGraphFactory): ModuleAnalyserS =
    new Implementation(classParser, classpathRoot, evidenceLimit, factory)

  private def sequence[T](xs: Seq[Try[T]]): Try[Seq[T]] = xs.foldLeft(Try(Seq[T]())) {
    (a, b) => a flatMap (c => b map (d => c :+ d))
  }
}
