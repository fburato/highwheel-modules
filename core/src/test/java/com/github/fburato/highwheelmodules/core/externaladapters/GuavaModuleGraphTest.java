package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleDependency;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GuavaModuleGraphTest {

    private final MutableNetwork<HWModule, ModuleDependency> graph = NetworkBuilder.directed().allowsSelfLoops(true)
            .build();
    private final GuavaModuleGraph testee = new GuavaModuleGraph(graph);

    private final HWModule m1 = HWModule.make("module a", "A").get();
    private final HWModule m2 = HWModule.make("module b", "B").get();
    private final HWModule m3 = HWModule.make("module c", "C").get();
    private final ModuleDependency dep = new ModuleDependency(m1, m2);

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
    void addDependencyShouldAddEdgeToJungGraph() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new ModuleDependency(m1, m2));

        final ModuleDependency dependency = graph.edgeConnecting(m1, m2).get();

        assertThat(dependency.source).isEqualTo(m1);
        assertThat(dependency.dest).isEqualTo(m2);
    }

    @Test
    void addEdgeShouldFailToAddIfOneVertexDoesNotExist() {
        testee.addModule(m1);
        testee.addDependency(new ModuleDependency(m1, m2));

        assertThat(graph.edges()).isEmpty();
    }

    @Test
    void addEdgeShouldIncreaseCounterIfDependencyAdded() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new ModuleDependency(m1, m2));

        assertThat(graph.edgeConnecting(m1, m2).get().getCount()).isEqualTo(1);
    }

    @Test
    void addEdgeShouldIncreaseCounterIfDependencyAddedMoreTimes() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new ModuleDependency(m1, m2));
        testee.addDependency(new ModuleDependency(m1, m2));

        assertThat(graph.edgeConnecting(m1, m2).get().getCount()).isEqualTo(2);
    }

    @Test
    void findEdgeShouldFindEdgeInExistingGraph() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new ModuleDependency(m1, m2));

        final Optional<ModuleDependency> dependencyOptional = testee.findDependency(m1, m2);
        assertThat(dependencyOptional.isPresent()).isTrue();

        final ModuleDependency dependency = dependencyOptional.get();
        assertThat(dependency.source).isEqualTo(m1);
        assertThat(dependency.dest).isEqualTo(m2);
    }

    @Test
    void findEdgeShouldReturnEmptyIfEdgeGoesInOppositeDirection() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new ModuleDependency(m1, m2));

        Optional<ModuleDependency> dependencyOptional = testee.findDependency(m2, m1);

        assertThat(dependencyOptional.isPresent()).isFalse();
    }

    @Test
    void findEdgeShouldReturnEmptyIfEdgeDoesNotExist() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addDependency(new ModuleDependency(m1, m2));

        Optional<ModuleDependency> dependencyOptional = testee.findDependency(m1, m3);

        assertThat(dependencyOptional.isPresent()).isFalse();
    }

    @Test
    void fanInOfModuleNotIntGraphShouldBeEmpty() {
        testee.addModule(m1);

        Optional<Integer> fanInM2 = testee.fanInOf(m2);

        assertThat(fanInM2.isPresent()).isFalse();
    }

    @Test
    void fanOutOfModuleNotInGraphShouldBeEmpty() {
        testee.addModule(m1);

        Optional<Integer> fanOutM2 = testee.fanInOf(m2);

        assertThat(fanOutM2.isPresent()).isFalse();
    }

    @Test
    void fanInOfModuleShouldEqualTheAmountOfIncomingEdges() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addModule(m3);

        testee.addDependency(new ModuleDependency(m2, m1));
        testee.addDependency(new ModuleDependency(m3, m1));

        assertThat(testee.fanInOf(m1).get()).isEqualTo(2);
    }

    @Test
    void fanOutOfModuleShouldEqualTheAmountOfOutgoingEdges() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addModule(m3);

        testee.addDependency(new ModuleDependency(m1, m2));
        testee.addDependency(new ModuleDependency(m1, m3));

        assertThat(testee.fanOutOf(m1).get()).isEqualTo(2);
    }

    @Test
    void fanInOfModuleShouldIgnoreMultipleCounters() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new ModuleDependency(m2, m1));
        testee.addDependency(new ModuleDependency(m2, m1));

        assertThat(testee.fanInOf(m1).get()).isEqualTo(1);
    }

    @Test
    void fanInOfModuleShouldIgnoreSelfDependencies() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new ModuleDependency(m2, m1));
        testee.addDependency(new ModuleDependency(m1, m1));

        assertThat(testee.fanInOf(m1).get()).isEqualTo(1);
    }

    @Test
    void fanOutOfModuleShouldIgnoreMultipleCounters() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new ModuleDependency(m1, m2));
        testee.addDependency(new ModuleDependency(m1, m2));

        assertThat(testee.fanOutOf(m1).get()).isEqualTo(1);
    }

    @Test
    void fanOutOfModuleShouldIgnoreSelfDependencies() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new ModuleDependency(m1, m2));
        testee.addDependency(new ModuleDependency(m1, m1));

        assertThat(testee.fanOutOf(m1).get()).isEqualTo(1);
    }

    @Test
    void dependenciesShouldReturnEmptyCollectionIfNothingConnectedToModule() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new ModuleDependency(m1, m2));

        assertThat(testee.dependencies(m2).isEmpty()).isTrue();
    }

    @Test
    void dependenciesShouldReturnEmptyCollectionIfModuleNotPresent() {
        testee.addModule(m1);
        testee.addModule(m2);

        testee.addDependency(new ModuleDependency(m1, m2));

        assertThat(testee.dependencies(m3).isEmpty()).isTrue();
    }

    @Test
    void dendenciesShouldReturnCollectionOfDependencies() {
        testee.addModule(m1);
        testee.addModule(m2);
        testee.addModule(m3);

        testee.addDependency(new ModuleDependency(m1, m2));
        testee.addDependency(new ModuleDependency(m1, m3));

        assertThat(testee.dependencies(m1)).contains(m2, m3);
    }
}
