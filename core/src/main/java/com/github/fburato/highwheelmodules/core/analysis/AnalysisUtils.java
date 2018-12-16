package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.model.modules.*;
import com.github.fburato.highwheelmodules.utils.Pair;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AnalysisUtils {
  public static Collection<HWModule> append(Collection<HWModule> modules, HWModule module) {
    final List<HWModule> result = new ArrayList<>(modules);
    result.add(module);
    return result;
  }


  public static List<List<Pair<String, String>>> getEvidence(DirectedGraph<HWModule, TrackingModuleDependency> trackingGraph, HWModule source, List<HWModule> path) {
    final List<HWModule> completePath = new ArrayList<>();
    completePath.add(source);
    completePath.addAll(path);
    final List<List<Pair<String, String>>> result = new ArrayList<>();
    for (int i = 0; i < completePath.size() - 1; ++i) {
      final HWModule current = completePath.get(i);
      final HWModule next = completePath.get(i + 1);
      final TrackingModuleDependency dependency = trackingGraph.findEdge(current, next);
      final List<Pair<String, String>> partial = new ArrayList<>();
      dependency.getSources().forEach((apSource) ->
          dependency.getDestinationsFromSource(apSource).forEach((apDest) -> partial.add(Pair.make(apSource.toString(), apDest.toString())))
      );
      result.add(partial);
    }
    return result;
  }

  public static List<String> getNames(Collection<HWModule> modules) {
    final List<String> result = new ArrayList<>(modules.size());
    for (HWModule module : modules) {
      result.add(module.name);
    }
    return result;
  }

  public static List<AnalyserModel.Metrics> getMetrics(ModuleMetrics moduleMetrics, Collection<HWModule> modules, ModuleGraph<ModuleDependency> graph,
                                                       HWModule other) {
    final List<AnalyserModel.Metrics> metrics = new ArrayList<>(modules.size());
    for (HWModule module : modules) {
      metrics.add(new AnalyserModel.Metrics(module.name, moduleMetrics.fanInOf(module).get() +
          (graph.findDependency(other, module).isPresent() ? -1 : 0),
          moduleMetrics.fanOutOf(module).get() + (
              graph.findDependency(module, other).isPresent() ? -1 : 0)));
    }
    return metrics;
  }
}
