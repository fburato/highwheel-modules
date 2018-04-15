package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.core.model.EvidenceModuleDependency;
import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.ModuleDependency;
import com.github.fburato.highwheelmodules.core.model.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.junit.Test;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.ElementName;

import java.util.Optional;

import static org.fest.assertions.api.Assertions.assertThat;

public class JungTrackingModuleGraphTest {

  private final DirectedGraph<Module, TrackingModuleDependency> graph = new DirectedSparseGraph<>();
  private final JungTrackingModuleGraph testee = new JungTrackingModuleGraph(graph);

  private final Module m1 = Module.make("module a", "A").get();
  private final Module m2 = Module.make("module b", "B").get();
  private final Module m3 = Module.make("module c", "C").get();
  private final AccessPoint ap1 = AccessPoint.create(ElementName.fromString("ap1"));
  private final AccessPoint ap2 = AccessPoint.create(ElementName.fromString("ap2"));
  private final AccessPoint ap3 = AccessPoint.create(ElementName.fromString("ap3"));
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
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    final TrackingModuleDependency dependency = graph.findEdge(m1, m2);

    assertThat(dependency.source).isEqualTo(m1);
    assertThat(dependency.dest).isEqualTo(m2);
  }

  @Test
  public void addEdgeShouldFailToAddIfOneVertexDoesNotExist() {
    testee.addModule(m1);
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    assertThat(graph.findEdge(m1, m2)).isNull();
  }

  @Test
  public void addEdgeShouldAddEdgeToDestinationMapping() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    assertThat(graph.findEdge(m1, m2).getDestinationsFromSource(ap1)).contains(ap2);
  }

  @Test
  public void addEdgeShouldAddEvidenceIfMappingAlreadyExistWithDifferentAccessPoint() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
    assertThat(graph.findEdge(m1, m2).getDestinationsFromSource(ap1)).contains(ap2);

    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap3));
    assertThat(graph.findEdge(m1, m2).getDestinationsFromSource(ap1)).contains(ap2, ap3);
  }

  @Test
  public void addEdgeShouldSkipIfMappingAlreadyExistWithSameAccessPoint() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
    assertThat(graph.findEdge(m1, m2).getDestinationsFromSource(ap1)).contains(ap2);

    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
    assertThat(graph.findEdge(m1, m2).getDestinationsFromSource(ap1)).contains(ap2);
    assertThat(graph.findEdge(m1, m2).getDestinationsFromSource(ap1).size()).isEqualTo(1);
  }

  @Test
  public void findEdgeShouldFindEdgeInExistingGraph() {
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
  public void findEdgeShouldReturnEmptyIfEdgeGoesInOppositeDirection() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    Optional<EvidenceModuleDependency> dependencyOptional = testee.findDependency(m2, m1);

    assertThat(dependencyOptional.isPresent()).isFalse();
  }

  @Test
  public void findEdgeShouldReturnEmptyIfEdgeDoesNotExist() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    Optional<EvidenceModuleDependency> dependencyOptional = testee.findDependency(m1, m3);

    assertThat(dependencyOptional.isPresent()).isFalse();
  }

  @Test
  public void dependenciesShouldReturnEmptyCollectionIfNothingConnectedToModule() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    assertThat(testee.dependencies(m2).isEmpty()).isTrue();
  }

  @Test
  public void dependenciesShouldReturnEmptyCollectionIfModuleNotPresent() {
    testee.addModule(m1);
    testee.addModule(m2);

    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));

    assertThat(testee.dependencies(m3).isEmpty()).isTrue();
  }

  @Test
  public void dendenciesShouldReturnCollectionOfDependencies() {
    testee.addModule(m1);
    testee.addModule(m2);
    testee.addModule(m3);

    testee.addDependency(new EvidenceModuleDependency(m1, m2, ap1, ap2));
    testee.addDependency(new EvidenceModuleDependency(m1, m3, ap1, ap2));

    assertThat(testee.dependencies(m1)).contains(m2, m3);
  }
}
