package com.github.fburato.highwheelmodules.core.model;

import org.pitest.highwheel.model.AccessPoint;

import java.util.*;

public final class TrackingModuleDependency {

  public final Module source;
  public final Module dest;
  private final Map<AccessPoint,Set<AccessPoint>> evidences;

  public TrackingModuleDependency(final Module source, final Module dest) {
    this.source = source;
    this.dest = dest;
    this.evidences = new HashMap<>();
  }

  public void addEvidence(AccessPoint source, AccessPoint dest) {
    final Set<AccessPoint> newDestinations = evidences.getOrDefault(source, new HashSet<>());
    newDestinations.add(dest);
    evidences.put(source,newDestinations);
  }

  public Set<AccessPoint> getSources() {
    return evidences.keySet();
  }

  public Set<AccessPoint> getDestinations() {
    final Set<AccessPoint> result = new HashSet<>();
    evidences.values().forEach(result::addAll);
    return result;
  }

  public Set<AccessPoint> getDestinationsFromSource(AccessPoint source) {
    return evidences.getOrDefault(source, Collections.emptySet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TrackingModuleDependency that = (TrackingModuleDependency) o;
    return Objects.equals(source, that.source) &&
        Objects.equals(dest, that.dest) &&
        Objects.equals(evidences, that.evidences);
  }

  @Override
  public int hashCode() {

    return Objects.hash(source, dest, evidences);
  }
}
