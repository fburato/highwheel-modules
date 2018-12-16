package com.github.fburato.highwheelmodules.core.externaladapters;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JungTrackingModuleGraphTest {


  private final DirectedGraph<HWModule, TrackingModuleDependency> graph = new DirectedSparseGraph<>();
  private final JungTrackingModuleGraph testee = new JungTrackingModuleGraph(graph);
  private final HWModule m1 = HWModule.make("module a", "A").get();
  private final HWModule m2 = HWModule.make("module b", "B").get();
  private final HWModule m3 = HWModule.make("module c", "C").get();
  private final AccessPoint ap1 = AccessPoint.create(ElementName.fromString("ap1"));
  private final AccessPoint ap2 = AccessPoint.create(ElementName.fromString("ap2"));
  private final AccessPoint ap3 = AccessPoint.create(ElementName.fromString("ap3"));

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
  public void addDependencyShouldCreateNewEdgeIfModulesDidNotExistAlready() {
    testee.addModule(m1);
    testee.addModule(m2);

    TrackingModuleDependency dep = new TrackingModuleDependency(m1,m2);
    dep.addEvidence(ap1,ap2);

    testee.addDependency(dep);

    assertThat(graph.findEdge(m1,m2)).isEqualTo(dep);
  }

  @Test
  public void addDependencyShouldMergeEvidencesFromDifferentAccessPoints(){
    testee.addModule(m1); testee.addModule(m2);

    TrackingModuleDependency dep1 = new TrackingModuleDependency(m1,m2);
    dep1.addEvidence(ap1,ap2);

    TrackingModuleDependency dep2 = new TrackingModuleDependency(m1,m2);
    dep2.addEvidence(ap2,ap3);

    testee.addDependency(dep1);
    testee.addDependency(dep2);

    assertThat(graph.findEdge(m1,m2).getDestinationsFromSource(ap1)).containsExactlyInAnyOrder(ap2);
    assertThat(graph.findEdge(m1,m2).getDestinationsFromSource(ap2)).containsExactlyInAnyOrder(ap3);
    assertThat(graph.findEdge(m1,m2).getDestinations()).containsExactlyInAnyOrder(ap2,ap3);
    assertThat(graph.findEdge(m1,m2).getSources()).containsExactlyInAnyOrder(ap1,ap2);
  }

  @Test
  public void addDependencyShouldMergeEvidencesFromSameAccessPoint(){
    testee.addModule(m1); testee.addModule(m2);

    TrackingModuleDependency dep1 = new TrackingModuleDependency(m1,m2);
    dep1.addEvidence(ap1,ap2);

    TrackingModuleDependency dep2 = new TrackingModuleDependency(m1,m2);
    dep2.addEvidence(ap1,ap3);
    testee.addDependency(dep1);
    testee.addDependency(dep2);

    assertThat(graph.findEdge(m1,m2).getDestinationsFromSource(ap1)).containsExactlyInAnyOrder(ap2,ap3);
    assertThat(graph.findEdge(m1,m2).getDestinations()).containsExactlyInAnyOrder(ap2,ap3);
    assertThat(graph.findEdge(m1,m2).getSources()).containsExactlyInAnyOrder(ap1);
  }

  @Test
  public void addDependencyShouldKeepEvidenceSeparatedForDifferentModules(){
    testee.addModule(m1); testee.addModule(m2); testee.addModule(m3);

    TrackingModuleDependency dep1 = new TrackingModuleDependency(m1,m2);
    dep1.addEvidence(ap1,ap2);

    TrackingModuleDependency dep2 = new TrackingModuleDependency(m1,m3);
    dep2.addEvidence(ap1,ap3);
    testee.addDependency(dep1);
    testee.addDependency(dep2);

    assertThat(graph.findEdge(m1,m2).getSources()).containsExactlyInAnyOrder(ap1);
    assertThat(graph.findEdge(m1,m2).getDestinations()).containsExactlyInAnyOrder(ap2);
    assertThat(graph.findEdge(m1,m3).getSources()).containsExactlyInAnyOrder(ap1);
    assertThat(graph.findEdge(m1,m3).getDestinations()).containsExactlyInAnyOrder(ap3);
  }

  @Test
  public void addDependencyShouldNotAddDependencyIfOneOfEdgesDoesNotExist() {
    testee.addModule(m1); testee.addModule(m2);

    TrackingModuleDependency dep = new TrackingModuleDependency(m1,m3);
    testee.addDependency(dep);

    assertThat(graph.getEdgeCount()).isEqualTo(0);
  }

  @Test
  public void dependenciesShouldReturnAllConnectedModules() {
    testee.addModule(m1); testee.addModule(m2); testee.addModule(m3);
    TrackingModuleDependency dep1 = new TrackingModuleDependency(m1,m2);
    TrackingModuleDependency dep2 = new TrackingModuleDependency(m1,m3);
    testee.addDependency(dep1);
    testee.addDependency(dep2);

    assertThat(testee.dependencies(m1)).containsExactlyInAnyOrder(m2,m3);
    assertThat(testee.dependencies(m2)).isEmpty();
  }
}
