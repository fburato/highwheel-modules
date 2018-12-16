package com.github.fburato.highwheelmodules.model.modules;

import java.util.Objects;

public final class ModuleDependency {

  public final HWModule source;
  public final HWModule dest;
  private int count;

  public ModuleDependency(HWModule source, HWModule dest) {
    this.source = source;
    this.dest = dest;
    this.count = 0;
  }

  public int getCount() {
    return count;
  }

  public void incrementCount() {
    count++;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ModuleDependency that = (ModuleDependency) o;

    return Objects.equals(this.source, that.source) &&
        Objects.equals(this.dest, that.dest) &&
        Objects.equals(this.count, that.count);
  }

  @Override
  public int hashCode() {
    int result = source.hashCode();
    result = 31 * result + dest.hashCode();
    result = 31 * result + count;
    return result;
  }
}
