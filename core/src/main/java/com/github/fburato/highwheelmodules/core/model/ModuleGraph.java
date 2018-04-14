package com.github.fburato.highwheelmodules.core.model;

import java.util.Collection;
import java.util.Optional;

public interface ModuleGraph<T> {
  Optional<T> findDependency(Module vertex1, Module vertex2);

  void addDependency(Module vertex1, Module vertex2);

  void addModule(Module vertex);

  Collection<Module> dependencies(Module vertex);
}
