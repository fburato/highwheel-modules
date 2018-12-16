package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.model.HWModule;
import com.github.fburato.highwheelmodules.core.model.ModuleGraph;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.*;

public class ModuleDependenciesGraphBuildingVisitor<T> implements AccessVisitor {

  public interface DependencyBuilder<G> {
    G build(HWModule m1, HWModule m2, AccessPoint source, AccessPoint dest, AccessType type);
  }

  private static class NoOpWarningsCollector implements WarningsCollector {
    @Override
    public void constructionWarning(HWModule m) {
    }

    @Override
    public void accessPointWarning(ElementName elementName, Collection<HWModule> message) {
    }
  }

  private final Collection<HWModule> modules;
  private final ModuleGraph<T> graph;
  private final WarningsCollector warningsCollector;
  private final HWModule other;
  private final DependencyBuilder<T> dependencyBuilder;

  public ModuleDependenciesGraphBuildingVisitor(
      final Collection<HWModule> modules,
      final ModuleGraph<T> graph,
      final HWModule other,
      final DependencyBuilder<T> dependencyBuilder,
      final WarningsCollector warningsCollector) {
    this.modules = modules;
    this.graph = graph;
    this.dependencyBuilder = dependencyBuilder;
    this.warningsCollector = warningsCollector;
    this.other = other;
    addModulesToGraph();
  }

  private void addModulesToGraph() {
    graph.addModule(other);
    final Set<String> processedModuleNames = new HashSet<String>(modules.size());
    for (HWModule module : modules) {
      graph.addModule(module);
      if (processedModuleNames.contains(module.name)) {
        warningsCollector.constructionWarning(module);
      }
      processedModuleNames.add(module.name);
    }
  }

  public ModuleDependenciesGraphBuildingVisitor(final Collection<HWModule> modules, final ModuleGraph<T> graph, final HWModule other, final DependencyBuilder<T> dependencyBuilder) {
    this(modules, graph, other, dependencyBuilder, new NoOpWarningsCollector());
  }

  @Override
  public void apply(AccessPoint source, AccessPoint dest, AccessType type) {
    final List<HWModule> modulesMatchingSource = getMatchingModules(source.getElementName());
    final List<HWModule> moduleMatchingDest = getMatchingModules(dest.getElementName());

    for (HWModule sourceModule : modulesMatchingSource) {
      for (HWModule destModule : moduleMatchingDest) {
        if (!sourceModule.equals(destModule))
          graph.addDependency(dependencyBuilder.build(sourceModule, destModule, source, dest, type));
      }
    }

    if (modulesMatchingSource.isEmpty() && !moduleMatchingDest.isEmpty()) {
      for (HWModule destModule : moduleMatchingDest) {
        graph.addDependency(dependencyBuilder.build(other, destModule, source, dest, type));
      }
    }

    if (!modulesMatchingSource.isEmpty() && moduleMatchingDest.isEmpty()) {
      for (HWModule sourceModule : modulesMatchingSource) {
        graph.addDependency(dependencyBuilder.build(sourceModule, other, source, dest, type));
      }
    }
  }

  private List<HWModule> getMatchingModules(ElementName name) {
    final List<HWModule> modulesMatchingName = new ArrayList<HWModule>(modules.size());
    for (HWModule module : modules) {
      if (module.contains(name)) {
        modulesMatchingName.add(module);
      }
    }
    if (modulesMatchingName.size() > 1) {
      warningsCollector.accessPointWarning(name, modulesMatchingName);
    }
    return modulesMatchingName;
  }

  @Override
  public void newNode(ElementName clazz) {

  }

  @Override
  public void newAccessPoint(AccessPoint ap) {

  }

  @Override
  public void newEntryPoint(ElementName clazz) {

  }
}
