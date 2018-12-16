package com.github.fburato.highwheelmodules.model.modules;

import org.junit.Test;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import static org.assertj.core.api.Assertions.assertThat;

public class TrackingModuleDependencyTest {

  private final HWModule moduleA = HWModule.make("module A", "module A").get();
  private final HWModule moduleB = HWModule.make("module B", "module B").get();
  private final AccessPoint exampleSource = AccessPoint.create(ElementName.fromString("A"));
  private final AccessPoint exampleSource1 = AccessPoint.create(ElementName.fromString("A1"));
  private final AccessPoint exampleDest = AccessPoint.create(ElementName.fromString("B"));
  private final AccessPoint exampleDest1 = AccessPoint.create(ElementName.fromString("A1"));
  private final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB);

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
    final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB);
    testee.addEvidence(exampleSource, exampleDest);
    testee.addEvidence(exampleSource, exampleDest1);

    assertThat(testee.getSources()).containsExactlyInAnyOrder(exampleSource);
    assertThat(testee.getDestinations()).containsExactlyInAnyOrder(exampleDest, exampleDest1);
    assertThat(testee.getEvidenceCounter()).isEqualTo(2);
  }

  @Test
  public void shouldSaveAllEvidenceDifferentSources() {
    final TrackingModuleDependency testee = new TrackingModuleDependency(moduleA, moduleB);
    testee.addEvidence(exampleSource, exampleDest);
    testee.addEvidence(exampleSource, exampleDest1);
    testee.addEvidence(exampleSource1, exampleDest);
    testee.addEvidence(exampleSource1, exampleDest1);

    assertThat(testee.getSources()).containsExactlyInAnyOrder(exampleSource, exampleSource1);
    assertThat(testee.getDestinations()).containsExactlyInAnyOrder(exampleDest, exampleDest1);
    assertThat(testee.getEvidenceCounter()).isEqualTo(4);
  }
}
