package com.github.fburato.highwheelmodules.core.model;

import org.junit.Test;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.ElementName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TrackingModuleDependencyTest {

  private final Module moduleA = Module.make("module A", "module A").get();
  private final Module moduleB = Module.make("module B", "module B").get();
  private final AccessPoint exampleSource = AccessPoint.create(ElementName.fromString("A"));
  private final AccessPoint exampleSource1 = AccessPoint.create(ElementName.fromString("A1"));
  private final AccessPoint exampleDest = AccessPoint.create(ElementName.fromString("B"));
  private final AccessPoint exampleDest1 = AccessPoint.create(ElementName.fromString("A1"));
  private final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB, Optional.empty());

  @Test
  public void evidencesShouldBeEmptyOnEmptyTrackingDependency() {
    assertThat(testee.getSources().isEmpty()).isTrue();
    assertThat(testee.getDestinations().isEmpty()).isTrue();
  }

  @Test
  public void sourcesShouldReturnAddedEvidences() {
    testee.addEvidence(exampleSource, exampleDest);

    assertThat(testee.getSources()).containsExactly(exampleSource);
    assertThat(testee.getDestinations()).containsExactly(exampleDest);
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

    assertThat(testee.getDestinationsFromSource(exampleSource)).containsExactly(exampleDest);
  }

  @Test
  public void shouldSaveAllEvidenceFromSameSource() {
    final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB, Optional.empty());
    testee.addEvidence(exampleSource, exampleDest);
    testee.addEvidence(exampleSource, exampleDest1);

    assertThat(testee.getSources()).containsExactlyInAnyOrder(exampleSource);
    assertThat(testee.getDestinations()).containsExactlyInAnyOrder(exampleDest, exampleDest1);
  }

  @Test
  public void shouldSaveAllEvidenceDifferentSources() {
    final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB, Optional.empty());
    testee.addEvidence(exampleSource, exampleDest);
    testee.addEvidence(exampleSource, exampleDest1);
    testee.addEvidence(exampleSource1, exampleDest);
    testee.addEvidence(exampleSource1, exampleDest1);

    assertThat(testee.getSources()).containsExactlyInAnyOrder(exampleSource, exampleSource1);
    assertThat(testee.getDestinations()).containsExactlyInAnyOrder(exampleDest, exampleDest1);
  }

  @Test
  public void shouldNotSaveMoreThanEvidenceLimitFromSameSource() {
    final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB, Optional.of(1));
    testee.addEvidence(exampleSource, exampleDest);
    testee.addEvidence(exampleSource, exampleDest1);

    assertThat(testee.getSources()).containsExactly(exampleSource);
    assertThat(testee.getDestinations()).containsExactly(exampleDest);
  }

  @Test
  public void shouldNotSaveMoreThanEvidenceLimitFromDifferentSources() {
    final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB, Optional.of(1));
    testee.addEvidence(exampleSource, exampleDest);
    testee.addEvidence(exampleSource, exampleDest1);
    testee.addEvidence(exampleSource1, exampleDest);
    testee.addEvidence(exampleSource1, exampleDest1);

    assertThat(testee.getSources()).containsExactly(exampleSource);
    assertThat(testee.getDestinations()).containsExactly(exampleDest);
  }
}