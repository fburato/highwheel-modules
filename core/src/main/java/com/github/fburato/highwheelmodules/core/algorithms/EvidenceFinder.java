package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import org.pitest.highwheel.model.AccessPoint;

import java.util.*;

public class EvidenceFinder {

  private final DirectedGraph<Module,TrackingModuleDependency> graph;
  public EvidenceFinder(DirectedGraph<Module,TrackingModuleDependency> graph) {
    this.graph = graph;
  }

  public List<AccessPoint> getDependencyEvidenceBetween(List<Module> modules) {
    if(! graph.getVertices().containsAll(modules)) {
      return Collections.emptyList();
    } else if(modules.size() == 1) {
      return Collections.emptyList();
    } else if(graph.findEdge(modules.get(0),modules.get(1)) == null) {
      return Collections.emptyList();
    } else {
      final TrackingModuleDependency accessPointsFromStart = graph.findEdge(modules.get(0),modules.get(1));
      for(AccessPoint ap: accessPointsFromStart.getSources()) {
        final List<AccessPoint> pathFromAp = getDependencyEvidenceBetween(modules,0,modules.size(),ap);
        if(!pathFromAp.isEmpty()) {
          return pathFromAp;
        }
      }
      return Collections.emptyList();
    }
  }

  private List<AccessPoint> getDependencyEvidenceBetween(List<Module> modules, int start, int end, AccessPoint ap) {
    if(start >= end - 1) {
      return Collections.emptyList();
    } else if(graph.findEdge(modules.get(start),modules.get(start+1)) == null) {
      return Collections.emptyList();
    } else if(start == end - 2) {
      final TrackingModuleDependency lastDependencyFromAp = graph.findEdge(modules.get(start),modules.get(start+1));
      if(lastDependencyFromAp.getDestinationsFromSource(ap).isEmpty()) {
        return Collections.emptyList();
      } else {
        return Arrays.asList(ap, new ArrayList<>(lastDependencyFromAp.getDestinationsFromSource(ap)).get(0));
      }
    } else {
      final TrackingModuleDependency dep = graph.findEdge(modules.get(start),modules.get(start+1));
      final Set<AccessPoint> nextAps = dep.getDestinationsFromSource(ap);
      for(AccessPoint inner: nextAps) {
        final List<AccessPoint> innerEvidenceThroughAp = getDependencyEvidenceBetween(modules,start+1,end,inner);
        if(!innerEvidenceThroughAp.isEmpty()) {
          final List<AccessPoint> result = new ArrayList<>(end - start + 1);
          result.add(ap);
          result.addAll(innerEvidenceThroughAp);
          return result;
        }
      }
      return Collections.emptyList();
    }
  }

}
