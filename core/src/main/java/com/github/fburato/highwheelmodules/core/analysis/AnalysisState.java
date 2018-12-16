package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.model.modules.*;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;

import java.util.Objects;

public class AnalysisState {
  public final MetricModuleGraph<ModuleDependency> specGraph;
  public final MetricModuleGraph<ModuleDependency> actualGraph;
  public final ModuleGraph<TrackingModuleDependency> actualTrackingGraph;
  public final AccessVisitor visitor;
  public final HWModule other;

  public AnalysisState(MetricModuleGraph<ModuleDependency> specGraph,
                       MetricModuleGraph<ModuleDependency> actualGraph,
                       ModuleGraph<TrackingModuleDependency> actualTrackingGraph,
                       AccessVisitor visitor,
                       HWModule other) {
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
