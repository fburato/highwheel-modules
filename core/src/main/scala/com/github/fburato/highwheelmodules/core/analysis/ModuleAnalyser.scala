package com.github.fburato.highwheelmodules.core.analysis

import com.github.fburato.highwheelmodules.core.algorithms.{CompoundAccessVisitor, ModuleDependenciesGraphBuildingVisitor}
import com.github.fburato.highwheelmodules.model.analysis.{LOOSE, STRICT}
import com.github.fburato.highwheelmodules.model.classpath.{AccessVisitor, ClassParser, ClasspathRoot}
import com.github.fburato.highwheelmodules.model.modules._
import com.github.fburato.highwheelmodules.utils.TryUtils._

import java.io.IOException
import scala.util.{Failure, Success, Try}

trait ModuleAnalyser {
  def analyse(definitions: Seq[DefinitionS]): Try[Seq[AnalysisResult]]
}

object ModuleAnalyser {

  private class Implementation(classParser: ClassParser, classpathRoot: ClasspathRoot, evidenceLimit: Option[Int], factory: ModuleGraphFactoryS) extends ModuleAnalyser {
    private val strictAnalyser = StrictAnalyser
    private val looseAnalyser = LooseAnalyser

    override def analyse(definitions: Seq[DefinitionS]): Try[Seq[AnalysisResult]] =
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

    private def initialiseState(definition: DefinitionS): Try[AnalysisState] = {
      val other = HWModuleS.make("(other)", List()).get

      def generateState(modules: Seq[HWModuleS]): AnalysisState = {
        val specModuleGraph = factory.buildMetricModuleGraph
        definition.modules.foreach(m => specModuleGraph.addModule(m))
        definition.dependencies.foreach(d => specModuleGraph.addDependency(ModuleDependencyS(d.source, d.dest)))
        val actualModuleGraph = factory.buildMetricModuleGraph
        val auxTrackingBareGraph = factory.buildTrackingModuleGraph
        val trackingGraph = factory.buildEvidenceModuleGraph(auxTrackingBareGraph, evidenceLimit)
        val moduleGraphVisitor = ModuleDependenciesGraphBuildingVisitor(modules, actualModuleGraph, other,
          (sourceModule, destModule, _, _, _) => ModuleDependencyS(sourceModule, destModule),
          definition.whitelist, definition.blacklist
        )
        val evidenceGraphVisitor = ModuleDependenciesGraphBuildingVisitor(modules, trackingGraph, other,
          (sourceModule, destModule, sourceAP, destAP, _) => EvidenceModuleDependencyS(sourceModule, destModule, sourceAP, destAP),
          definition.whitelist, definition.blacklist
        )
        val accessVisitor = CompoundAccessVisitor(Seq(moduleGraphVisitor, evidenceGraphVisitor))
        AnalysisState(modules, definition.dependencies, definition.noStrictDependencies, specModuleGraph, actualModuleGraph, auxTrackingBareGraph, accessVisitor, other)
      }

      definition.modules match {
        case Seq() => Failure(AnalyserException("No modules provided in definition"))
        case s => Success(generateState(s))
      }
    }

    private def visitorAndProcessors(definitions: Seq[DefinitionS]): Try[(AccessVisitor, Seq[() => AnalysisResult])] = {
      val merged = definitions.map(d =>
        for {
          state <- initialiseState(d)
        } yield (state.visitor, () => d.mode match {
          case STRICT => strictAnalyser.analyse(state)
          case LOOSE => looseAnalyser.analyse(state)
        })
      )
      sequence(merged).map(sequence => {
        val (visitors, processors) = sequence.unzip
        (CompoundAccessVisitor(visitors), processors)
      })
    }
  }

  def apply(classParser: ClassParser, classpathRoot: ClasspathRoot, evidenceLimit: Option[Int], factory: ModuleGraphFactoryS): ModuleAnalyser =
    new Implementation(classParser, classpathRoot, evidenceLimit, factory)
}
