package com.github.fburato.highwheelmodules.core.model;

import org.pitest.highwheel.model.AccessPoint;

import java.util.Objects;

public final class EvidenceModuleDependency {
  public final Module sourceModule;
  public final Module destModule;
  public final AccessPoint source;
  public final AccessPoint dest;

  public EvidenceModuleDependency(Module sourceModule, Module destModule, AccessPoint source, AccessPoint dest) {
    this.sourceModule = sourceModule;
    this.destModule = destModule;
    this.source = source;
    this.dest = dest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EvidenceModuleDependency that = (EvidenceModuleDependency) o;
    return Objects.equals(sourceModule, that.sourceModule) &&
        Objects.equals(destModule, that.destModule) &&
        Objects.equals(source, that.source) &&
        Objects.equals(dest, that.dest);
  }

  @Override
  public int hashCode() {

    return Objects.hash(sourceModule, destModule, source, dest);
  }
}
