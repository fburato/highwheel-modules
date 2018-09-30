package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.CompoundAccessVisitor;
import com.github.fburato.highwheelmodules.core.algorithms.ModuleDependenciesGraphBuildingVisitor;
import com.github.fburato.highwheelmodules.core.externaladapters.JungModuleGraph;
import com.github.fburato.highwheelmodules.core.externaladapters.JungTrackingModuleGraph;
import com.github.fburato.highwheelmodules.core.model.*;
import com.github.fburato.highwheelmodules.core.model.rules.Dependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.pitest.highwheel.classpath.AccessVisitor;

import java.util.Collection;
import java.util.Optional;

public class DefinitionVisitor {

  private final Optional<Integer> evidenceLimit;

  public DefinitionVisitor(Optional<Integer> evidenceLimit) {
    this.evidenceLimit = evidenceLimit;
  }

  public AnalysisState getAnalysisState(Definition definition) {
    final Module other = Module.make("(other)", "").get();
    final Collection<Module> modules = definition.modules;
    if (modules.isEmpty())
      throw new AnalyserException("No modules provided in definition");
    final JungModuleGraph specModuleGraph = initialiseSpecificationGraph(modules, definition.dependencies);
    final JungModuleGraph actualModuleGraph = initialiseEmptyGraph();
    final DirectedSparseGraph<Module, TrackingModuleDependency> trackingBareGraph = new DirectedSparseGraph<>();
    final JungTrackingModuleGraph trackingGraph = new JungTrackingModuleGraph(trackingBareGraph, evidenceLimit);
    final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<ModuleDependency> moduleGraphBuilder =
        (sourceModule, destModule, sourceAP, destAP, type) -> new ModuleDependency(sourceModule, destModule);
    final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<EvidenceModuleDependency> evidenceGraphBuilder =
        (sourceModule, destModule, sourceAP, destAP, type) -> new EvidenceModuleDependency(sourceModule, destModule, sourceAP, destAP);
    final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> moduleGraphVisitor =
        new ModuleDependenciesGraphBuildingVisitor<>(modules, actualModuleGraph, other, moduleGraphBuilder);
    final ModuleDependenciesGraphBuildingVisitor<EvidenceModuleDependency> evidenceGraphVisitor =
        new ModuleDependenciesGraphBuildingVisitor<>(modules, trackingGraph, other, evidenceGraphBuilder);
    final AccessVisitor accessVisitor = new CompoundAccessVisitor(moduleGraphVisitor, evidenceGraphVisitor);
    return new AnalysisState(specModuleGraph,actualModuleGraph,trackingBareGraph,accessVisitor,other);
  }

  private JungModuleGraph initialiseSpecificationGraph(Collection<Module> modules, Collection<Dependency> dependencies) {
    final DirectedGraph<Module, ModuleDependency> specGraph = new DirectedSparseGraph<>();
    final JungModuleGraph specModuleGraph = new JungModuleGraph(specGraph);

    for (Module module : modules) {
      specModuleGraph.addModule(module);
    }
    for (Dependency dep : dependencies) {
      specModuleGraph.addDependency(new ModuleDependency(dep.source, dep.dest));
    }

    return specModuleGraph;
  }

  private JungModuleGraph initialiseEmptyGraph() {
    final DirectedGraph<Module, ModuleDependency> actualGraph = new DirectedSparseGraph<>();
    return new JungModuleGraph(actualGraph);
  }
}
