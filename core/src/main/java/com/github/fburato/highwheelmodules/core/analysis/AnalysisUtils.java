package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.model.*;
import com.github.fburato.highwheelmodules.utils.Pair;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AnalysisUtils {
  public static Collection<Module> append(Collection<Module> modules, Module module) {
    final List<Module> result = new ArrayList<>(modules);
    result.add(module);
    return result;
  }


  public static List<List<Pair<String, String>>> getEvidence(DirectedGraph<Module, TrackingModuleDependency> trackingGraph, Module source, List<Module> path) {
    final List<Module> completePath = new ArrayList<>();
    completePath.add(source);
    completePath.addAll(path);
    final List<List<Pair<String, String>>> result = new ArrayList<>();
    for (int i = 0; i < completePath.size() - 1; ++i) {
      final Module current = completePath.get(i);
      final Module next = completePath.get(i + 1);
      final TrackingModuleDependency dependency = trackingGraph.findEdge(current, next);
      final List<Pair<String, String>> partial = new ArrayList<>();
      dependency.getSources().forEach((apSource) ->
          dependency.getDestinationsFromSource(apSource).forEach((apDest) -> partial.add(Pair.make(apSource.toString(), apDest.toString())))
      );
      result.add(partial);
    }
    return result;
  }

  public static List<String> getNames(Collection<Module> modules) {
    final List<String> result = new ArrayList<>(modules.size());
    for (Module module : modules) {
      result.add(module.name);
    }
    return result;
  }

  public static List<AnalyserModel.Metrics> getMetrics(ModuleMetrics moduleMetrics, Collection<Module> modules, ModuleGraph<ModuleDependency> graph,
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
}
