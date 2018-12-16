package com.github.fburato.highwheelmodules.model.rules;


import com.github.fburato.highwheelmodules.model.modules.HWModule;

import java.util.Objects;

public class Dependency implements Rule {
  public final HWModule source;
  public final HWModule dest;

  public Dependency(HWModule source, HWModule dest) {
    this.source = source;
    this.dest = dest;
  }

  @Override
  public String toString() {
    return String.format("%s -> %s", source.name, dest.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Dependency that = (Dependency) o;

    return Objects.equals(this.source, that.source) &&
        Objects.equals(this.dest, that.dest);
  }

}
