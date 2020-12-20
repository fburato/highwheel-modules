package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.core.algorithms.{CompoundAccessVisitor, ModuleDependenciesGraphBuildingVisitorS}
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode
import com.github.fburato.highwheelmodules.model.classpath.{AccessVisitorS, ClassParserS, ClasspathRootS}
import com.github.fburato.highwheelmodules.model.modules._
import com.github.fburato.highwheelmodules.utils.TryUtils._

import java.io.IOException
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.{Failure, Success, Try}

trait ModuleAnalyser {
  def analyse(definitions: Seq[Definition]): Try[Seq[AnalysisResult]]
}

object ModuleAnalyser {

  private class Implementation(classParser: ClassParserS, classpathRoot: ClasspathRootS, evidenceLimit: Option[Int], factory: ModuleGraphFactory) extends ModuleAnalyser {
    private val strictAnalyser = StrictAnalyser
    private val looseAnalyser = LooseAnalyser

    override def analyse(definitions: Seq[Definition]): Try[Seq[AnalysisResult]] =
      if (definitions.isEmpty) {
        Success(Seq())
      } else {
        for {
          visitorsAndProcessors <- visitorAndProcessors(definitions)
          (visitor, processors) = visitorsAndProcessors
          _ <- classParser.parse(classpathRoot, visitor) recoverWith {
            case e: IOException => Failure(AnalyserException(e))
          }
          results <- sequence(processors.map(p => Try(p())))
        } yield results
      }

    private def initialiseState(definition: Definition): Try[AnalysisState] = {
      val other = HWModule.make("(other)", "").get

      def generateState(modules: Seq[HWModule]): AnalysisState = {
        val specModuleGraph = factory.buildMetricModuleGraph()
        definition.modules.forEach(m => specModuleGraph.addModule(m))
        definition.dependencies.forEach(d => specModuleGraph.addDependency(new ModuleDependency(d.source, d.dest)))
        val actualModuleGraph = factory.buildMetricModuleGraph()
        val auxTrackingBareGraph = factory.buildTrackingModuleGraph()
        val trackingGraph = factory.buildEvidenceModuleGraph(auxTrackingBareGraph, evidenceLimit.map(i => new Integer(i)).toJava)
        val moduleGraphVisitor = ModuleDependenciesGraphBuildingVisitorS(modules, actualModuleGraph, other,
          (sourceModule, destModule, _, _, _) => new ModuleDependency(sourceModule, destModule),
          definition.whitelist.toScala, definition.blackList.toScala
        )
        val evidenceGraphVisitor = ModuleDependenciesGraphBuildingVisitorS(modules, trackingGraph, other,
          (sourceModule, destModule, sourceAP, destAP, _) => new EvidenceModuleDependency(sourceModule, destModule, sourceAP, destAP),
          definition.whitelist.toScala, definition.blackList.toScala
        )
        val accessVisitor = CompoundAccessVisitor(Seq(moduleGraphVisitor, evidenceGraphVisitor))
        AnalysisState(modules, definition.dependencies.asScala.toSeq, definition.noStrictDependencies.asScala.toSeq, specModuleGraph, actualModuleGraph, auxTrackingBareGraph, accessVisitor, other)
      }

      definition.modules.asScala.toSeq match {
        case Seq() => Failure(AnalyserException("No modules provided in definition"))
        case s => Success(generateState(s))
      }
    }

    private def visitorAndProcessors(definitions: Seq[Definition]): Try[(AccessVisitorS, Seq[() => AnalysisResult])] = {
      val merged = definitions.map(d =>
        for {
          state <- initialiseState(d)
        } yield (state.visitor, () => d.mode match {
          case AnalysisMode.STRICT => strictAnalyser.analyse(state)
          case AnalysisMode.LOOSE => looseAnalyser.analyse(state)
        })
      )
      sequence(merged).map(sequence => {
        val (visitors, processors) = sequence.unzip
        (CompoundAccessVisitor(visitors), processors)
      })
    }
  }

  def apply(classParser: ClassParserS, classpathRoot: ClasspathRootS, evidenceLimit: Option[Int], factory: ModuleGraphFactory): ModuleAnalyser =
    new Implementation(classParser, classpathRoot, evidenceLimit, factory)
}
