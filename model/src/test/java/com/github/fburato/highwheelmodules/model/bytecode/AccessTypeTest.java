package com.github.fburato.highwheelmodules.model.bytecode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessTypeTest {

  @Test
  public void shouldTreatInheritanceAsStrongerRelationshipThanComposure() {
    assertThat(AccessType.COMPOSED.getStrength() < AccessType.INHERITANCE.getStrength()).isTrue();
  }

}
