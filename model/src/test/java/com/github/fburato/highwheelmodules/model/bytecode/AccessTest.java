package com.github.fburato.highwheelmodules.model.bytecode;


import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.jupiter.api.Test;

public class AccessTest {

  @Test
  public void shouldObeyHashcodeEqualsContract() {
    EqualsVerifier.forClass(Access.class).verify();
  }


}
