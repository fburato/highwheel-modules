package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class JungTrackingModuleGraph implements ModuleGraph<TrackingModuleDependency> {

    private final DirectedGraph<HWModule, TrackingModuleDependency> graph;

    public JungTrackingModuleGraph(DirectedGraph<HWModule, TrackingModuleDependency> graph) {
        this.graph = graph;
    }

    @Override
    public Optional<TrackingModuleDependency> findDependency(HWModule vertex1, HWModule vertex2) {
        return Optional.ofNullable(graph.findEdge(vertex1, vertex2));
    }

    @Override
    public void addDependency(TrackingModuleDependency dependency) {
        if (graph.getVertices().containsAll(Arrays.asList(dependency.source, dependency.dest))) {
            final TrackingModuleDependency dep = Optional.ofNullable(graph.findEdge(dependency.source, dependency.dest))
                    .orElseGet(() -> {
                        final TrackingModuleDependency newDep = new TrackingModuleDependency(dependency.source,
                                dependency.dest);
                        graph.addEdge(newDep, dependency.source, dependency.dest);
                        return newDep;
                    });
            for (AccessPoint s : dependency.getSources()) {
                for (AccessPoint d : dependency.getDestinationsFromSource(s)) {
                    dep.addEvidence(s, d);
                }
            }
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
}
