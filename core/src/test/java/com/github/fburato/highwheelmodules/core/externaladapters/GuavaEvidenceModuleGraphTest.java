package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.EvidenceModuleDependency;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleDependency;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.junit.jupiter.api.Test;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GuavaEvidenceModuleGraphTest {

    private final MutableNetwork<HWModule, TrackingModuleDependency> graph = NetworkBuilder.directed()
            .allowsSelfLoops(true).build();
    private final GuavaTrackingModuleGraph aux = new GuavaTrackingModuleGraph(graph);
    private final GuavaEvidenceModuleGraph testee = new GuavaEvidenceModuleGraph(aux, Optional.empty());

    private final HWModule m1 = HWModule.make("module a", "A").get();
    private final HWModule m2 = HWModule.make("module b", "B").get();
    private final HWModule m3 = HWModule.make("module c", "C").get();
    private final AccessPoint ap1 = AccessPoint.create(ElementName.fromString("ap1"));
    private final AccessPoint ap2 = AccessPoint.create(ElementName.fromString("ap2"));
    private final AccessPoint ap3 = AccessPoint.create(ElementName.fromString("ap3"));
    private final ModuleDependency dep = new ModuleDependency(m1, m2);

    @Test
    void addModuleShouldAddVertexToJungGraph() {
        testee.addModule(m1);

        assertThat(graph.nodes().contains(m1)).isTrue();
    }

    @Test
    void addModuleShouldFailIfVertexAlreadyAdded() {
        testee.addModule(m1);
        testee.addModule(m1);

        assertThat(graph.nodes().size()).isEqualTo(1);
    }

    @Test
    void addDependencyShouldAddEdgeToJungGraph() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        final TrackingModuleDependency dependency = graph.edgeConnecting(m1, m2).get();

        assertThat(dependency.source).isEqualTo(m1);
        assertThat(dependency.dest).isEqualTo(m2);
    }

    @Test
    void addDependencyShouldBuildTrackingDependencyWithNoEvidenceLimitIfGraphIsInitialisedWithoutLimit() {
        final GuavaEvidenceModuleGraph otherTestee = new GuavaEvidenceModuleGraph(aux, Optional.empty());
        otherTestee.addModule(m1);
        otherTestee.addModule(m2);
        otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
        otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3));

        final TrackingModuleDependency dependency = graph.edgeConnecting(m1, m2).get();

        assertThat(dependency.source).isEqualTo(m1);
        assertThat(dependency.dest).isEqualTo(m2);
        assertThat(dependency.getDestinations()).containsExactlyInAnyOrder(ap2, ap3);
    }

    @Test
    void addDependencyShouldBuildTrackingDependencyWithEvidenceLimitIfGraphIsInitialisedWithLimit() {
        final GuavaEvidenceModuleGraph otherTestee = new GuavaEvidenceModuleGraph(aux, Optional.of(1));
        otherTestee.addModule(m1);
        otherTestee.addModule(m2);
        otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
        otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3));

        final TrackingModuleDependency dependency = graph.edgeConnecting(m1, m2).get();

        assertThat(dependency.source).isEqualTo(m1);
        assertThat(dependency.dest).isEqualTo(m2);
        assertThat(dependency.getDestinations()).containsExactly(ap2);
    }

    @Test
    void addDependencyShouldKeepTrackOfDependenciesIfLimitOfDependencyIs0() {
        final GuavaEvidenceModuleGraph otherTestee = new GuavaEvidenceModuleGraph(aux, Optional.of(0));
        otherTestee.addModule(m1);
        otherTestee.addModule(m2);
        otherTestee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        final TrackingModuleDependency dependency = graph.edgeConnecting(m1, m2).get();

        assertThat(dependency.source).isEqualTo(m1);
        assertThat(dependency.dest).isEqualTo(m2);
        assertThat(dependency.getSources()).isEmpty();
        assertThat(dependency.getSources()).isEmpty();
    }

    @Test
    void addEdgeShouldFailToAddIfOneVertexDoesNotExist() {
        testee.addModule(m1);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        assertThat(graph.edges()).isEmpty();
    }

    @Test
    void addEdgeShouldAddEdgeToDestinationMapping() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).contains(ap2);
    }

    @Test
    void addEdgeShouldAddEvidenceIfMappingAlreadyExistWithDifferentAccessPoint() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).contains(ap2);

        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3));
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).contains(ap2, ap3);
    }

    @Test
    void addEdgeShouldSkipIfMappingAlreadyExistWithSameAccessPoint() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).contains(ap2);

        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1)).contains(ap2);
        assertThat(graph.edgeConnecting(m1, m2).get().getDestinationsFromSource(ap1).size()).isEqualTo(1);
    }

    @Test
    void findEdgeShouldFindEdgeInExistingGraph() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        final Optional<EvidenceModuleDependency> dependencyOptional = testee.findDependency(m1, m2);
        assertThat(dependencyOptional.isPresent()).isTrue();

        final EvidenceModuleDependency dependency = dependencyOptional.get();
        assertThat(dependency.sourceModule).isEqualTo(m1);
        assertThat(dependency.destModule).isEqualTo(m2);
        assertThat(dependency.source).isEqualTo(ap1);
        assertThat(dependency.dest).isEqualTo(ap2);
    }

    @Test
    void findEdgeShouldReturnEmptyIfEdgeGoesInOppositeDirection() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        Optional<EvidenceModuleDependency> dependencyOptional = testee.findDependency(m2, m1);

        assertThat(dependencyOptional.isPresent()).isFalse();
    }

    @Test
    void findEdgeShouldReturnEmptyIfEdgeDoesNotExist() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        Optional<EvidenceModuleDependency> dependencyOptional = testee.findDependency(m1, m3);

        assertThat(dependencyOptional.isPresent()).isFalse();
    }

    @Test
    void dependenciesShouldReturnEmptyCollectionIfNothingConnectedToModule() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        assertThat(testee.dependencies(m2).isEmpty()).isTrue();
    }

    @Test
    void dependenciesShouldReturnEmptyCollectionIfModuleNotPresent() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

        assertThat(testee.dependencies(m3).isEmpty()).isTrue();
    }

    @Test
    void dependenciesShouldReturnCollectionOfDependencies() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addModule(m3);

        testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
        testee.addDependency(new EvidenceModuleDependency(m1, m3, ap1, ap2));

        assertThat(testee.dependencies(m1)).contains(m2, m3);
    }
}
