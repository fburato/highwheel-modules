package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;

import java.util.*;

public final class TrackingModuleDependency {

  public final HWModule source;
  public final HWModule dest;
  private final Map<AccessPoint, Set<AccessPoint>> evidences;
  private final Optional<Integer> evidenceLimit;
  private int evidenceCounter = 0;

  public TrackingModuleDependency(final HWModule source, final HWModule dest, final Optional<Integer> evidenceLimit) {
    this.source = source;
    this.dest = dest;
    this.evidences = new HashMap<>();
    this.evidenceLimit = evidenceLimit;
  }

  public void addEvidence(AccessPoint source, AccessPoint dest) {
    if (!evidenceLimit.isPresent() || evidenceCounter < evidenceLimit.get()) {
      final Set<AccessPoint> newDestinations = evidences.getOrDefault(source, new HashSet<>());
      newDestinations.add(dest);
      evidences.put(source, newDestinations);
      evidenceCounter++;
    }
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
