package com.github.fburato.highwheelmodules.core;

import com.github.fburato.highwheelmodules.core.algorithms.ModuleDependenciesGraphBuildingVisitor;
import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure;
import com.github.fburato.highwheelmodules.core.externaladapters.JungModuleGraph;
import com.github.fburato.highwheelmodules.core.model.*;
import com.github.fburato.highwheelmodules.core.model.rules.Dependency;
import com.github.fburato.highwheelmodules.core.model.rules.NoStrictDependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.pitest.highwheel.classpath.ClassParser;
import org.pitest.highwheel.classpath.ClasspathRoot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModuleAnalyser {

  private final ClassParser classParser;

  public ModuleAnalyser(final ClassParser classParser) {
    this.classParser = classParser;
  }

  public AnalyserModel.StrictAnalysisResult analyseStrict(final ClasspathRoot root, final Definition definition) {
    final Module other = Module.make("(other)", "").get();
    final Collection<Module> modules = definition.modules;
    if (modules.isEmpty())
      throw new AnalyserException("No modules provided in definition");
    final JungModuleGraph specModuleGraph = initialiseSpecificationGraph(modules, definition.dependencies);
    final JungModuleGraph actualModuleGraph = initialiseEmptyGraph();

    runAnalysis(modules, actualModuleGraph, root, other);

    final ModuleGraphTransitiveClosure specTransitiveClosure =
        new ModuleGraphTransitiveClosure(specModuleGraph, append(modules, other));
    final ModuleGraphTransitiveClosure actualTransitiveClosure =
        new ModuleGraphTransitiveClosure(actualModuleGraph, append(modules, other));

    final List<AnalyserModel.DependencyViolation> dependencyViolations =
        getDependencyViolations(specTransitiveClosure.diffPath(actualTransitiveClosure).get(), other);
    final List<AnalyserModel.NoStrictDependencyViolation> noStrictDependencyViolations =
        getNoDirectDependecyViolations(actualTransitiveClosure, definition.noStrictDependencies, other);
    final List<AnalyserModel.Metrics> metrics = getMetrics(actualModuleGraph, modules, actualModuleGraph, other);

    return new AnalyserModel.StrictAnalysisResult(dependencyViolations, noStrictDependencyViolations, metrics);
  }

  private Collection<Module> append(Collection<Module> modules, Module module) {
    final List<Module> result = new ArrayList<>(modules);
    result.add(module);
    return result;
  }

  private JungModuleGraph initialiseSpecificationGraph(Collection<Module> modules, Collection<Dependency> dependencies) {
    final DirectedGraph<Module, ModuleDependency> specGraph = new DirectedSparseGraph<Module, ModuleDependency>();
    final JungModuleGraph specModuleGraph = new JungModuleGraph(specGraph);

    for (Module module : modules) {
      specModuleGraph.addModule(module);
    }
    for (Dependency dep : dependencies) {
      specModuleGraph.addDependency(new ModuleDependency(dep.source,dep.dest));
    }

    return specModuleGraph;
  }

  private JungModuleGraph initialiseEmptyGraph() {
    final DirectedGraph<Module, ModuleDependency> actualGraph = new DirectedSparseGraph<Module, ModuleDependency>();
    return new JungModuleGraph(actualGraph);
  }

  private void runAnalysis(Collection<Module> modules, ModuleGraph<ModuleDependency> moduleGraph, ClasspathRoot root, Module other) {
    final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<ModuleDependency>  builder = (sourceModule, destModule, sourceAP, destAP, type) -> new ModuleDependency(sourceModule,destModule);
    final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> visitor =
        new ModuleDependenciesGraphBuildingVisitor<>(modules, moduleGraph, other, builder);

    try {
      classParser.parse(root, visitor);
    } catch (IOException e) {
      throw new AnalyserException(e);
    }
  }

  private List<AnalyserModel.DependencyViolation> getDependencyViolations(
      List<ModuleGraphTransitiveClosure.PathDifference> differences, Module other) {
    final List<AnalyserModel.DependencyViolation> dependencyViolations =
        new ArrayList<AnalyserModel.DependencyViolation>(differences.size());
    for (ModuleGraphTransitiveClosure.PathDifference difference : differences) {
      if (!difference.source.equals(other) && !difference.dest.equals(other)) {
        dependencyViolations.add(
            new AnalyserModel.DependencyViolation(difference.source.name, difference.dest.name, getNames(difference.firstPath),
                getNames(difference.secondPath)));
      }
    }
    return dependencyViolations;
  }

  private List<AnalyserModel.NoStrictDependencyViolation> getNoDirectDependecyViolations(
      ModuleGraphTransitiveClosure transitiveClosure, Collection<NoStrictDependency> rules, Module other) {
    final List<AnalyserModel.NoStrictDependencyViolation> noStrictDependencyViolations =
        new ArrayList<AnalyserModel.NoStrictDependencyViolation>();
    for (NoStrictDependency rule : rules) {
      if (!rule.source.equals(other) && !rule.dest.equals(other)
          && transitiveClosure.minimumDistance(rule.source, rule.dest).get() == 1) {
        noStrictDependencyViolations.add(new AnalyserModel.NoStrictDependencyViolation(rule.source.name, rule.dest.name));
      }
    }
    return noStrictDependencyViolations;
  }

  private List<AnalyserModel.Metrics> getMetrics(ModuleMetrics moduleMetrics, Collection<Module> modules, ModuleGraph<ModuleDependency> graph,
      Module other) {
    final List<AnalyserModel.Metrics> metrics = new ArrayList<>(modules.size());
    for (Module module : modules) {
      metrics.add(new AnalyserModel.Metrics(module.name, moduleMetrics.fanInOf(module).get() +
          (graph.findDependency(other, module).isPresent() ? -1 : 0),
          moduleMetrics.fanOutOf(module).get() + (
              graph.findDependency(module, other).isPresent() ? -1 : 0)));
    }
    return metrics;
  }

  private static List<String> getNames(Collection<Module> modules) {
    final List<String> result = new ArrayList<String>(modules.size());
    for (Module module : modules) {
      result.add(module.name);
    }
    return result;
  }

  public AnalyserModel.LooseAnalysisResult analyseLoose(final ClasspathRoot root, final Definition definition) {
    final Collection<Module> modules = definition.modules;
    final Module other = Module.make("(other)", "").get();
    if (modules.isEmpty())
      throw new AnalyserException("No modules provided in definition");
    final JungModuleGraph actualModuleGraph = initialiseEmptyGraph();

    runAnalysis(modules, actualModuleGraph, root, other);

    final ModuleGraphTransitiveClosure actualTransitiveClosure =
        new ModuleGraphTransitiveClosure(actualModuleGraph, append(modules, other));

    final List<AnalyserModel.AbsentDependencyViolation> absentDependencyViolations =
        getAbsentDependencies(modules, actualTransitiveClosure, definition.dependencies, other);
    final List<AnalyserModel.UndesiredDependencyViolation> undesiredDependencyViolations =
        getUndesiredDependecies(actualTransitiveClosure, definition.noStrictDependencies, other);

    return new AnalyserModel.LooseAnalysisResult(absentDependencyViolations, undesiredDependencyViolations,
        getMetrics(actualModuleGraph, modules, actualModuleGraph, other));
  }

  private List<AnalyserModel.AbsentDependencyViolation> getAbsentDependencies(Collection<Module> modules,
      ModuleGraphTransitiveClosure transitiveClosure,
      Collection<Dependency> dependencies, Module other) {
    final List<AnalyserModel.AbsentDependencyViolation> dependencyViolations =
        new ArrayList<AnalyserModel.AbsentDependencyViolation>();
    for (Dependency dependency : dependencies) {
      if (!dependency.source.equals(other) && !dependency.equals(other) && !transitiveClosure
          .isReachable(dependency.source, dependency.dest)) {
        dependencyViolations.add(new AnalyserModel.AbsentDependencyViolation(dependency.source.name, dependency.dest.name));
      }
    }

    return dependencyViolations;
  }

  private List<AnalyserModel.UndesiredDependencyViolation> getUndesiredDependecies(
      ModuleGraphTransitiveClosure transitiveClosure, Collection<NoStrictDependency> noStrictDependencies, Module other) {
    final List<AnalyserModel.UndesiredDependencyViolation> undesiredDependencyViolations =
        new ArrayList<AnalyserModel.UndesiredDependencyViolation>();
    for (NoStrictDependency noStrictDependency : noStrictDependencies) {
      if (!noStrictDependency.source.equals(other) && !noStrictDependency.dest.equals(other) && transitiveClosure
          .isReachable(noStrictDependency.source, noStrictDependency.dest)) {
        undesiredDependencyViolations.add(new AnalyserModel.UndesiredDependencyViolation(noStrictDependency.source.name,
            noStrictDependency.dest.name,
            getNames(
                transitiveClosure.minimumDistancePath(
                    noStrictDependency.source, noStrictDependency.dest
                )
            )
        ));
      }
    }
    return undesiredDependencyViolations;
  }
}
