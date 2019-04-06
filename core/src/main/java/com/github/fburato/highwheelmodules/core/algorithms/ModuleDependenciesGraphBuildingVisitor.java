package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.model.modules.AnonymousModule;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
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
    private final Optional<AnonymousModule> whiteList;
    private final Optional<AnonymousModule> blackList;

    public ModuleDependenciesGraphBuildingVisitor(final Collection<HWModule> modules, final ModuleGraph<T> graph,
            final HWModule other, final DependencyBuilder<T> dependencyBuilder,
            final WarningsCollector warningsCollector) {
        this(modules, graph, other, dependencyBuilder, warningsCollector, Optional.empty(), Optional.empty());
    }

    public ModuleDependenciesGraphBuildingVisitor(final Collection<HWModule> modules, final ModuleGraph<T> graph,
            final HWModule other, final DependencyBuilder<T> dependencyBuilder,
            final WarningsCollector warningsCollector, final Optional<AnonymousModule> whiteList,
            final Optional<AnonymousModule> blackList) {
        this.modules = modules;
        this.graph = graph;
        this.dependencyBuilder = dependencyBuilder;
        this.warningsCollector = warningsCollector;
        this.other = other;
        this.whiteList = whiteList;
        this.blackList = blackList;
        addModulesToGraph();
    }

    private void addModulesToGraph() {
        graph.addModule(other);
        final Set<String> processedModuleNames = new HashSet<>(modules.size());
        for (HWModule module : modules) {
            graph.addModule(module);
            if (processedModuleNames.contains(module.name)) {
                warningsCollector.constructionWarning(module);
            }
            processedModuleNames.add(module.name);
        }
    }

    public ModuleDependenciesGraphBuildingVisitor(final Collection<HWModule> modules, final ModuleGraph<T> graph,
            final HWModule other, final DependencyBuilder<T> dependencyBuilder) {
        this(modules, graph, other, dependencyBuilder, new NoOpWarningsCollector());
    }

    public ModuleDependenciesGraphBuildingVisitor(final Collection<HWModule> modules, final ModuleGraph<T> graph,
            final HWModule other, final DependencyBuilder<T> dependencyBuilder,
            final Optional<AnonymousModule> whiteList, final Optional<AnonymousModule> blackList) {
        this(modules, graph, other, dependencyBuilder, new NoOpWarningsCollector(), whiteList, blackList);
    }

    @Override
    public void apply(AccessPoint source, AccessPoint dest, AccessType type) {
        if (filterWhiteBlack(source.getElementName(), dest.getElementName())) {
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
    }

    private boolean filterWhiteBlack(final ElementName source, final ElementName dest) {
        final boolean bothInWhiteList = whiteList.map(matcher -> matcher.contains(source) && matcher.contains(dest))
                .orElse(true);
        final boolean bothNotInBlackList = blackList
                .map(matcher -> !matcher.contains(source) && !matcher.contains(dest)).orElse(true);
        return bothInWhiteList && bothNotInBlackList;
    }

    private List<HWModule> getMatchingModules(ElementName name) {
        final List<HWModule> modulesMatchingName = new ArrayList<>(modules.size());
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
