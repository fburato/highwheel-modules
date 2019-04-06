package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JungModuleGraphTest {

  private final DirectedGraph<HWModule, ModuleDependency> graph = new DirectedSparseGraph<>();
  private final JungModuleGraph testee = new JungModuleGraph(graph);

  private final HWModule m1 = HWModule.make("module a", "A").get();
  private final HWModule m2 = HWModule.make("module b", "B").get();
  private final HWModule m3 = HWModule.make("module c", "C").get();
  private final ModuleDependency dep = new ModuleDependency(m1, m2);

  @Test
  public void addModuleShouldAddVertexToJungGraph() {
    testee.addModule(m1);

    assertThat(graph.getVertices().contains(m1)).isTrue();
  }

  @Test
  public void addModuleShouldFailIfVertexAlreadyAdded() {
    testee.addModule(m1);
    testee.addModule(m1);

    assertThat(graph.getVertices().size()).isEqualTo(1);
  }

  @Test
  public void addDependencyShouldAddEdgeToJungGraph() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new ModuleDependency(m1, m2));

    final ModuleDependency dependency = graph.findEdge(m1, m2);

    assertThat(dependency.source).isEqualTo(m1);
    assertThat(dependency.dest).isEqualTo(m2);
  }

  @Test
  public void addEdgeShouldFailToAddIfOneVertexDoesNotExist() {
    testee.addModule(m1);
    testee.addDependency(new ModuleDependency(m1, m2));

    assertThat(graph.findEdge(m1, m2)).isNull();
  }

  @Test
  public void addEdgeShouldIncreaseCounterIfDependencyAdded() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new ModuleDependency(m1, m2));

    assertThat(graph.findEdge(m1, m2).getCount()).isEqualTo(1);
  }

  @Test
  public void addEdgeShouldIncreaseCounterIfDependencyAddedMoreTimes() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new ModuleDependency(m1, m2));
    testee.addDependency(new ModuleDependency(m1, m2));

    assertThat(graph.findEdge(m1, m2).getCount()).isEqualTo(2);
  }

  @Test
  public void findEdgeShouldFindEdgeInExistingGraph() {
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
  public void findEdgeShouldReturnEmptyIfEdgeGoesInOppositeDirection() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new ModuleDependency(m1, m2));

    Optional<ModuleDependency> dependencyOptional = testee.findDependency(m2, m1);

    assertThat(dependencyOptional.isPresent()).isFalse();
  }

  @Test
  public void findEdgeShouldReturnEmptyIfEdgeDoesNotExist() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new ModuleDependency(m1, m2));

    Optional<ModuleDependency> dependencyOptional = testee.findDependency(m1, m3);

    assertThat(dependencyOptional.isPresent()).isFalse();
  }

  @Test
  public void fanInOfModuleNotIntGraphShouldBeEmpty() {
    testee.addModule(m1);

    Optional<Integer> fanInM2 = testee.fanInOf(m2);

    assertThat(fanInM2.isPresent()).isFalse();
  }

  @Test
  public void fanOutOfModuleNotInGraphShouldBeEmpty() {
    testee.addModule(m1);

    Optional<Integer> fanOutM2 = testee.fanInOf(m2);

    assertThat(fanOutM2.isPresent()).isFalse();
  }

  @Test
  public void fanInOfModuleShouldEqualTheAmountOfIncomingEdges() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addModule(m3);

    testee.addDependency(new ModuleDependency(m2, m1));
    testee.addDependency(new ModuleDependency(m3, m1));

    assertThat(testee.fanInOf(m1).get()).isEqualTo(2);
  }

  @Test
  public void fanOutOfModuleShouldEqualTheAmountOfOutgoingEdges() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addModule(m3);

    testee.addDependency(new ModuleDependency(m1, m2));
    testee.addDependency(new ModuleDependency(m1, m3));

    assertThat(testee.fanOutOf(m1).get()).isEqualTo(2);
  }

  @Test
  public void fanInOfModuleShouldIgnoreMultipleCounters() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new ModuleDependency(m2, m1));
    testee.addDependency(new ModuleDependency(m2, m1));

    assertThat(testee.fanInOf(m1).get()).isEqualTo(1);
  }

  @Test
  public void fanInOfModuleShouldIgnoreSelfDependencies() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new ModuleDependency(m2, m1));
    testee.addDependency(new ModuleDependency(m1, m1));

    assertThat(testee.fanInOf(m1).get()).isEqualTo(1);
  }

  @Test
  public void fanOutOfModuleShouldIgnoreMultipleCounters() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new ModuleDependency(m1, m2));
    testee.addDependency(new ModuleDependency(m1, m2));

    assertThat(testee.fanOutOf(m1).get()).isEqualTo(1);
  }

  @Test
  public void fanOutOfModuleShouldIgnoreSelfDependencies() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new ModuleDependency(m1, m2));
    testee.addDependency(new ModuleDependency(m1, m1));

    assertThat(testee.fanOutOf(m1).get()).isEqualTo(1);
  }

  @Test
  public void dependenciesShouldReturnEmptyCollectionIfNothingConnectedToModule() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new ModuleDependency(m1, m2));

    assertThat(testee.dependencies(m2).isEmpty()).isTrue();
  }

  @Test
  public void dependenciesShouldReturnEmptyCollectionIfModuleNotPresent() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new ModuleDependency(m1, m2));

    assertThat(testee.dependencies(m3).isEmpty()).isTrue();
  }

  @Test
  public void dendenciesShouldReturnCollectionOfDependencies() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addModule(m3);

    testee.addDependency(new ModuleDependency(m1, m2));
    testee.addDependency(new ModuleDependency(m1, m3));

    assertThat(testee.dependencies(m1)).contains(m2, m3);
  }
}
