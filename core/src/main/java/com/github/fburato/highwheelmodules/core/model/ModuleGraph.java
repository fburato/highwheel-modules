package com.github.fburato.highwheelmodules.core.model;

import java.util.Collection;
import java.util.Optional;

public interface ModuleGraph<T> {
  Optional<T> findDependency(HWModule vertex1, HWModule vertex2);

  void addDependency(T dependency);

  void addModule(HWModule vertex);

  Collection<HWModule> dependencies(HWModule vertex);
}
