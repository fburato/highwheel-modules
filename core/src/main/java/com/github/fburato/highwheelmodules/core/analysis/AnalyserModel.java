package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.utils.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface AnalyserModel {

  class DependencyViolation {
    public final String sourceModule;
    public final String destinationModule;
    public final List<String> specificationPath;
    public final List<String> actualPath;
    public final List<List<Pair<String, String>>> evidences;

    public DependencyViolation(String sourceModule, String destinationModule, List<String> specificationPath,
                               List<String> actualPath) {
      this(sourceModule, destinationModule, specificationPath, actualPath, Collections.emptyList());
    }

    public DependencyViolation(String sourceModule, String destinationModule, List<String> specificationPath,
                               List<String> actualPath, List<List<Pair<String, String>>> evidences) {
      this.sourceModule = sourceModule;
      this.destinationModule = destinationModule;
      this.specificationPath = specificationPath;
      this.actualPath = actualPath;
      this.evidences = evidences;
    }

    @Override
    public String toString() {
      return "DependencyViolation{" +
          "sourceModule='" + sourceModule + '\'' +
          ", destinationModule='" + destinationModule + '\'' +
          ", specificationPath=" + specificationPath +
          ", actualPath=" + actualPath +
          ", evidences=" + evidences +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DependencyViolation that = (DependencyViolation) o;
      return Objects.equals(sourceModule, that.sourceModule) &&
          Objects.equals(destinationModule, that.destinationModule) &&
          Objects.equals(specificationPath, that.specificationPath) &&
          Objects.equals(actualPath, that.actualPath) &&
          Objects.equals(evidences, that.evidences);
    }

    @Override
    public int hashCode() {

      return Objects.hash(sourceModule, destinationModule, specificationPath, actualPath, evidences);
    }
  }

  class NoStrictDependencyViolation {
    public final String sourceModule;
    public final String destinationModule;

    public NoStrictDependencyViolation(String sourceModule, String destinationModule) {
      this.sourceModule = sourceModule;
      this.destinationModule = destinationModule;
    }

    @Override
    public String toString() {
      return sourceModule + " -/-> " + destinationModule;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      NoStrictDependencyViolation that = (NoStrictDependencyViolation) o;

      return Objects.equals(this.sourceModule, that.sourceModule) &&
          Objects.equals(this.destinationModule, that.destinationModule);
    }
  }

  class Metrics {
    public final String module;
    public final int fanIn;
    public final int fanOut;

    public Metrics(String module, int fanIn, int fanOut) {
      this.module = module;
      this.fanIn = fanIn;
      this.fanOut = fanOut;
    }

    @Override
    public String toString() {
      return "Metrics{" +
          "module='" + module + '\'' +
          ", fanIn=" + fanIn +
          ", fanOut=" + fanOut +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      Metrics that = (Metrics) o;

      return Objects.equals(this.module, that.module) &&
          Objects.equals(this.fanIn, that.fanIn) &&
          Objects.equals(this.fanOut, that.fanOut);
    }

    @Override
    public int hashCode() {
      int result = module != null ? module.hashCode() : 0;
      result = 31 * result + fanIn;
      result = 31 * result + fanOut;
      return result;
    }
  }

  class AbsentDependencyViolation {
    public final String sourceModule;
    public final String destinationModule;

    public AbsentDependencyViolation(String sourceModule, String destinationModule) {
      this.sourceModule = sourceModule;
      this.destinationModule = destinationModule;
    }

    @Override
    public String toString() {
      return sourceModule + " -/-> " + destinationModule;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      AbsentDependencyViolation that = (AbsentDependencyViolation) o;

      return Objects.equals(this.sourceModule, that.sourceModule) &&
          Objects.equals(this.destinationModule, that.destinationModule);
    }
  }

  class UndesiredDependencyViolation {
    public final String sourceModule;
    public final String destinationModule;
    public final List<String> moduleEvidence;
    public final List<List<Pair<String, String>>> evidences;

    public UndesiredDependencyViolation(String sourceModule, String destinationModule, List<String> moduleEvidence) {
      this(sourceModule, destinationModule, moduleEvidence, Collections.emptyList());
    }

    public UndesiredDependencyViolation(String sourceModule, String destinationModule, List<String> moduleEvidence, List<List<Pair<String, String>>> evidences) {
      this.sourceModule = sourceModule;
      this.destinationModule = destinationModule;
      this.moduleEvidence = moduleEvidence;
      this.evidences = evidences;
    }

    @Override
    public String toString() {
      return "UndesiredDependencyViolation{" +
          "sourceModule='" + sourceModule + '\'' +
          ", destinationModule='" + destinationModule + '\'' +
          ", moduleEvidence=" + moduleEvidence +
          ", evidences=" + evidences +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UndesiredDependencyViolation that = (UndesiredDependencyViolation) o;
      return Objects.equals(sourceModule, that.sourceModule) &&
          Objects.equals(destinationModule, that.destinationModule) &&
          Objects.equals(moduleEvidence, that.moduleEvidence) &&
          Objects.equals(evidences, that.evidences);
    }

    @Override
    public int hashCode() {

      return Objects.hash(sourceModule, destinationModule, moduleEvidence, evidences);
    }
  }

  class StrictAnalysisResult {
    public final Collection<DependencyViolation> dependencyViolations;
    public final Collection<NoStrictDependencyViolation> noStrictDependencyViolations;
    public final Collection<Metrics> metrics;

    public StrictAnalysisResult(Collection<DependencyViolation> dependencyViolations,
                                Collection<NoStrictDependencyViolation> noStrictDependencyViolations, Collection<Metrics> metrics) {
      this.dependencyViolations = dependencyViolations;
      this.noStrictDependencyViolations = noStrictDependencyViolations;
      this.metrics = metrics;
    }
  }

  class LooseAnalysisResult {
    public final Collection<AbsentDependencyViolation> absentDependencyViolations;
    public final Collection<UndesiredDependencyViolation> undesiredDependencyViolations;
    public final Collection<Metrics> metrics;

    public LooseAnalysisResult(Collection<AbsentDependencyViolation> absentDependencyViolations,
                               Collection<UndesiredDependencyViolation> undesiredDependencyViolations, Collection<Metrics> metrics) {
      this.absentDependencyViolations = absentDependencyViolations;
      this.undesiredDependencyViolations = undesiredDependencyViolations;
      this.metrics = metrics;
    }
  }
}
