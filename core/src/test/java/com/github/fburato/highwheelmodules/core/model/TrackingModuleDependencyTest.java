package com.github.fburato.highwheelmodules.core.model;

import org.junit.Test;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.ElementName;

import static org.fest.assertions.api.Assertions.assertThat;

public class TrackingModuleDependencyTest {

  private final Module moduleA = Module.make("module A", "module A").get();
  private final Module moduleB = Module.make("module B", "module B").get();
  private final AccessPoint exampleSource = AccessPoint.create(ElementName.fromString("A"));
  private final AccessPoint exampleDest = AccessPoint.create(ElementName.fromString("B"));
  private final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB);

  @Test
  public void evidencesShouldBeEmptyOnEmptyTrackingDependency() {
    assertThat(testee.getSources().isEmpty()).isTrue();
    assertThat(testee.getDestinations().isEmpty()).isTrue();
  }

  @Test
  public void sourcesShouldReturnAddedEvidences() {
    testee.addEvidence(exampleSource, exampleDest);

    assertThat(testee.getSources()).contains(exampleSource);
    assertThat(testee.getDestinations()).contains(exampleDest);
  }

  @Test
  public void getDestinationShouldReturnEmptyOnNotAddedEvidences() {
    assertThat(testee.getDestinationsFromSource(exampleSource).isEmpty()).isTrue();
  }

  @Test
  public void getDestinationShouldReturnEmptyOnEvidenceOnDifferentSource() {
    testee.addEvidence(exampleDest, exampleDest);

    assertThat(testee.getDestinationsFromSource(exampleSource).isEmpty()).isTrue();
  }

  @Test
  public void getDestinationShouldReturnExpectedEvidences() {
    testee.addEvidence(exampleSource, exampleDest);

    assertThat(testee.getDestinationsFromSource(exampleSource)).contains(exampleDest);
  }
}
