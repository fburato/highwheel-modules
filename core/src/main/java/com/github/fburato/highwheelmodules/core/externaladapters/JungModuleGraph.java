package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.*;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class JungModuleGraph implements MetricModuleGraph<ModuleDependency> {

    private final DirectedGraph<HWModule, ModuleDependency> graph;

    public JungModuleGraph(DirectedGraph<HWModule, ModuleDependency> graph) {
        this.graph = graph;
    }

    @Override
    public Optional<ModuleDependency> findDependency(HWModule vertex1, HWModule vertex2) {
        return Optional.ofNullable(graph.findEdge(vertex1, vertex2));
    }

    @Override
    public void addDependency(final ModuleDependency dependency) {
        if (graph.getVertices().containsAll(Arrays.asList(dependency.source, dependency.dest))) {
            Optional<ModuleDependency> dependencyOptional = Optional
                    .ofNullable(graph.findEdge(dependency.source, dependency.dest));
            final ModuleDependency moduleDependency = dependencyOptional.orElseGet(() -> {
                graph.addEdge(dependency, dependency.source, dependency.dest);
                return dependency;
            });
            moduleDependency.incrementCount();
        }
    }

    @Override
    public void addModule(HWModule vertex) {
        graph.addVertex(vertex);
    }

    @Override
    public Collection<HWModule> modules() {
        return graph.getVertices();
    }

    @Override
    public Collection<HWModule> dependencies(HWModule vertex) {
        return Optional.ofNullable(graph.getSuccessors(vertex)).orElse(Collections.emptyList());
    }

    @Override
    public Optional<Integer> fanInOf(HWModule module) {
        if (!graph.containsVertex(module))
            return Optional.empty();
        else {
            final Optional<ModuleDependency> self = findDependency(module, module);
            return Optional.of(graph.inDegree(module) - self.map((a) -> 1).orElse(0));
        }
    }

    @Override
    public Optional<Integer> fanOutOf(HWModule module) {
        if (!graph.containsVertex(module))
            return Optional.empty();
        else {
            final Optional<ModuleDependency> self = findDependency(module, module);
            return Optional.of(graph.outDegree(module) - self.map((a) -> 1).orElse(0));
        }
    }
}
