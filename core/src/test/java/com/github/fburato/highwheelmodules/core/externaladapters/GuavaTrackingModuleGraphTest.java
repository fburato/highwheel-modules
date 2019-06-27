package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuavaTrackingModuleGraphTest {

    private final MutableNetwork<HWModule, TrackingModuleDependency> graph = NetworkBuilder.directed()
            .allowsSelfLoops(true).build();
    private final GuavaTrackingModuleGraph testee = new GuavaTrackingModuleGraph(graph);
    private final HWModule m1 = HWModule.make("module a", "A").get();
    private final HWModule m2 = HWModule.make("module b", "B").get();
    private final HWModule m3 = HWModule.make("module c", "C").get();
    private final AccessPoint ap1 = AccessPoint.create(ElementName.fromString("ap1"));
    private final AccessPoint ap2 = AccessPoint.create(ElementName.fromString("ap2"));
    private final AccessPoint ap3 = AccessPoint.create(ElementName.fromString("ap3"));

    @Test
    void addModuleShouldAddVertexToJungGraph() {
        testee.addModule(m1);

        assertThat(graph.nodes()).contains(m1);
    }

    @Test
    void addModuleShouldFailIfVertexAlreadyAdded() {
        testee.addModule(m1);
        testee.addModule(m1);

        assertThat(graph.nodes().size()).isEqualTo(1);
    }

    @Test
    void addDependencyShouldCreateNewEdgeIfModulesDidNotExistAlready() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep = new TrackingModuleDependency(m1, m2);
        dep.addEvidence(ap1, ap2);

        testee.addDependency(dep);

        assertThat(graph.edgeConnecting(m1, m2)).contains(dep);
    }

    @Test
    void addDependencyShouldMergeEvidencesFromDifferentAccessPoints() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep1 = new TrackingModuleDependency(m1, m2);
        dep1.addEvidence(ap1, ap2);

        TrackingModuleDependency dep2 = new TrackingModuleDependency(m1, m2);
        dep2.addEvidence(ap2, ap3);

        testee.addDependency(dep1);
        testee.addDependency(dep2);

        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).containsExactlyInAnyOrder(ap2);
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap2)).containsExactlyInAnyOrder(ap3);
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinations()).containsExactlyInAnyOrder(ap2, ap3);
        assertThat(graph.edgeConnecting(m1, m2).get().getSources()).containsExactlyInAnyOrder(ap1, ap2);
    }

    @Test
    void addDependencyShouldNotAddEvidencesIfAlreadyPresent() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep1 = new TrackingModuleDependency(m1, m2);
        dep1.addEvidence(ap1, ap2);

        TrackingModuleDependency dep2 = new TrackingModuleDependency(m1, m2);
        dep2.addEvidence(ap1, ap2);

        testee.addDependency(dep1);
        testee.addDependency(dep2);

        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).containsExactlyInAnyOrder(ap2);
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinations()).containsExactlyInAnyOrder(ap2);
        assertThat(graph.edgeConnecting(m1, m2).get().getSources()).containsExactlyInAnyOrder(ap1);
    }

    @Test
    void addDependencyShouldMergeEvidencesFromSameAccessPoint() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep1 = new TrackingModuleDependency(m1, m2);
        dep1.addEvidence(ap1, ap2);

        TrackingModuleDependency dep2 = new TrackingModuleDependency(m1, m2);
        dep2.addEvidence(ap1, ap3);
        testee.addDependency(dep1);
        testee.addDependency(dep2);

        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).containsExactlyInAnyOrder(ap2,
                ap3);
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinations()).containsExactlyInAnyOrder(ap2, ap3);
        assertThat(graph.edgeConnecting(m1, m2).get().getSources()).containsExactlyInAnyOrder(ap1);
    }

    @Test
    void addDependencyShouldKeepEvidenceSeparatedForDifferentModules() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addModule(m3);

        TrackingModuleDependency dep1 = new TrackingModuleDependency(m1, m2);
        dep1.addEvidence(ap1, ap2);

        TrackingModuleDependency dep2 = new TrackingModuleDependency(m1, m3);
        dep2.addEvidence(ap1, ap3);
        testee.addDependency(dep1);
        testee.addDependency(dep2);

        assertThat(graph.edgeConnecting(m1, m2).get().getSources()).containsExactlyInAnyOrder(ap1);
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinations()).containsExactlyInAnyOrder(ap2);
        assertThat(graph.edgeConnecting(m1, m3).get().getSources()).containsExactlyInAnyOrder(ap1);
        assertThat(graph.edgeConnecting(m1, m3).get().getDestinations()).containsExactlyInAnyOrder(ap3);
    }

    @Test
    void addDependencyShouldNotAddDependencyIfOneOfEdgesDoesNotExist() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep = new TrackingModuleDependency(m1, m3);
        testee.addDependency(dep);

        assertThat(graph.edges().size()).isEqualTo(0);
    }

    @Test
    void dependenciesShouldReturnAllConnectedModules() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addModule(m3);
        TrackingModuleDependency dep1 = new TrackingModuleDependency(m1, m2);
        TrackingModuleDependency dep2 = new TrackingModuleDependency(m1, m3);
        testee.addDependency(dep1);
        testee.addDependency(dep2);

        assertThat(testee.dependencies(m1)).containsExactlyInAnyOrder(m2, m3);
        assertThat(testee.dependencies(m2)).isEmpty();
    }

    @Test
    void findDependencyShouldReturnExpectedDependency() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep = new TrackingModuleDependency(m1, m2);
        dep.addEvidence(ap1, ap2);

        testee.addDependency(dep);

        assertThat(testee.findDependency(m1, m2)).contains(dep);
    }

    @Test
    void findDependencyShouldReturnEmptyIfDependencyNotDefined() {
        testee.addModule(m1);
        testee.addModule(m2);

        TrackingModuleDependency dep = new TrackingModuleDependency(m1, m2);
        dep.addEvidence(ap1, ap2);

        testee.addDependency(dep);

        assertThat(testee.findDependency(m1, m1)).isEmpty();
    }
}
