package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.externaladapters.JungModuleGraph;
import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.pitest.highwheel.classpath.AccessVisitor;

import java.util.Objects;

public class AnalysisState {
  public final JungModuleGraph specGraph;
  public final JungModuleGraph actualGraph;
  public final DirectedSparseGraph<Module, TrackingModuleDependency> actualTrackingGraph;
  public final AccessVisitor visitor;
  public final Module other;

  public AnalysisState(JungModuleGraph specGraph,
                       JungModuleGraph actualGraph,
                       DirectedSparseGraph<Module, TrackingModuleDependency> actualTrackingGraph,
                       AccessVisitor visitor,
                       Module other) {
    this.specGraph = specGraph;
    this.actualGraph = actualGraph;
    this.actualTrackingGraph = actualTrackingGraph;
    this.visitor = visitor;
    this.other = other;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AnalysisState)) return false;
    AnalysisState that = (AnalysisState) o;
    return Objects.equals(specGraph, that.specGraph) &&
        Objects.equals(actualGraph, that.actualGraph) &&
        Objects.equals(visitor, that.visitor) &&
        Objects.equals(other, that.other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(specGraph, actualGraph, visitor, other);
  }

  @Override
  public String toString() {
    return "AnalysisState{" +
        "specGraph=" + specGraph +
        ", actualGraph=" + actualGraph +
        ", visitor=" + visitor +
        ", other=" + other +
        '}';
  }
}
