package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.model.Module;
import com.github.fburato.highwheelmodules.core.model.TrackingModuleDependency;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.junit.Before;
import org.junit.Test;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.ElementName;

import java.util.Arrays;

import static org.fest.assertions.api.Assertions.assertThat;

public class EvidenceFinderTest {

  private final Module m1 = Module.make("a","a").get();
  private final Module m2 = Module.make("b", "b").get();
  private final Module m3 = Module.make("c", "c").get();
  private final Module m4 = Module.make("d","d").get();
  private final AccessPoint ap1 = AccessPoint.create(ElementName.fromString("ap1"));
  private final AccessPoint ap2 = AccessPoint.create(ElementName.fromString("ap2"));
  private final AccessPoint ap3 = AccessPoint.create(ElementName.fromString("ap3"));
  private final AccessPoint ap4 = AccessPoint.create(ElementName.fromString("ap4"));
  private final DirectedGraph<Module,TrackingModuleDependency> graph = new DirectedSparseGraph<>();
  private final EvidenceFinder testee = new EvidenceFinder(graph);

  @Before
  public void setUp() {
    graph.addVertex(m1);
    graph.addVertex(m2);
    graph.addVertex(m3);
    final TrackingModuleDependency t1 = new TrackingModuleDependency(m1,m2);
    final TrackingModuleDependency t2 = new TrackingModuleDependency(m2,m3);
    t1.addEvidence(ap1,ap2);
    t1.addEvidence(ap3,ap4);
    t2.addEvidence(ap4,ap3);
    graph.addEdge(t1,m1,m2);
    graph.addEdge(t2,m2,m3);
  }
  @Test
  public void shouldReturnEmptyListIfModuleVertexDoesNotExist() {
    assertThat(testee.getDependencyEvidenceBetween(Arrays.asList(m1,m4)).isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnEmptyListIfDependencyDoesNotExist() {
    assertThat(testee.getDependencyEvidenceBetween(Arrays.asList(m3,m2)).isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnValidAccessPointSequenceBetweenTwoVertices() {
    assertThat(testee.getDependencyEvidenceBetween(Arrays.asList(m2,m3))).isEqualTo(Arrays.asList(ap4,ap3));
  }

  @Test
  public void shouldReturnValidAccessPointSequenceBetweenMoreThanTwoVertices() {
    assertThat(testee.getDependencyEvidenceBetween(Arrays.asList(m1,m2,m3))).isEqualTo(Arrays.asList(ap3,ap4,ap3));
  }

}
