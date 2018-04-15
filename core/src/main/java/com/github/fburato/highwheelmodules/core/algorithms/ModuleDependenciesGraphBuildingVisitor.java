package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.ModuleGraph;
import org.pitest.highwheel.classpath.AccessVisitor;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.AccessType;
import org.pitest.highwheel.model.ElementName;

import java.util.*;

public class ModuleDependenciesGraphBuildingVisitor<T> implements AccessVisitor {

  public interface DependencyBuilder<G> {
    G build(Module m1, Module m2, AccessPoint source, AccessPoint dest, AccessType type);
  }

  private static class NoOpWarningsCollector implements WarningsCollector {
    @Override
    public void constructionWarning(Module m) {
    }

    @Override
    public void accessPointWarning(ElementName elementName, Collection<Module> message) {
    }
  }

  private final Collection<Module> modules;
  private final ModuleGraph<T> graph;
  private final WarningsCollector warningsCollector;
  private final Module other;
  private final DependencyBuilder<T> dependencyBuilder;

  public ModuleDependenciesGraphBuildingVisitor(
      final Collection<Module> modules,
      final ModuleGraph<T> graph,
      final Module other,
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
    for (Module module : modules) {
      graph.addModule(module);
      if (processedModuleNames.contains(module.name)) {
        warningsCollector.constructionWarning(module);
      }
      processedModuleNames.add(module.name);
    }
  }

  public ModuleDependenciesGraphBuildingVisitor(final Collection<Module> modules, final ModuleGraph<T> graph, final Module other, final DependencyBuilder<T> dependencyBuilder) {
    this(modules, graph, other, dependencyBuilder, new NoOpWarningsCollector());
  }

  @Override
  public void apply(AccessPoint source, AccessPoint dest, AccessType type) {
    final List<Module> modulesMatchingSource = getMatchingModules(source.getElementName());
    final List<Module> moduleMatchingDest = getMatchingModules(dest.getElementName());

    for (Module sourceModule : modulesMatchingSource) {
      for (Module destModule : moduleMatchingDest) {
        if (!sourceModule.equals(destModule))
          graph.addDependency(dependencyBuilder.build(sourceModule, destModule, source, dest, type));
      }
    }

    if (modulesMatchingSource.isEmpty() && !moduleMatchingDest.isEmpty()) {
      for (Module destModule : moduleMatchingDest) {
        graph.addDependency(dependencyBuilder.build(other, destModule, source, dest, type));
      }
    }

    if (!modulesMatchingSource.isEmpty() && moduleMatchingDest.isEmpty()) {
      for (Module sourceModule : modulesMatchingSource) {
        graph.addDependency(dependencyBuilder.build(sourceModule, other, source, dest, type));
      }
    }
  }

  private List<Module> getMatchingModules(ElementName name) {
    final List<Module> modulesMatchingName = new ArrayList<Module>(modules.size());
    for (Module module : modules) {
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
