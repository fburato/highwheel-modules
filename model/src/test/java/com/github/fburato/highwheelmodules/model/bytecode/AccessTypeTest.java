package com.github.fburato.highwheelmodules.model.bytecode;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessTypeTest {

  @Test
  public void shouldTreatInheritanceAsStrongerRelationshipThanComposure() {
    assertTrue(AccessType.COMPOSED.getStrength() < AccessType.INHERITANCE.getStrength());
  }

}
