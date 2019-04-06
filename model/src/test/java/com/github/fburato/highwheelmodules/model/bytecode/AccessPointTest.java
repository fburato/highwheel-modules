package com.github.fburato.highwheelmodules.model.bytecode;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessPointTest {

  private final ElementName foo = ElementName.fromString("foo");

  @Test
  public void shouldObeyHashcodeEqualsContract() {
    EqualsVerifier.forClass(AccessPoint.class).verify();
  }

  @Test
  public void shouldCreateAnAccessPoint() {
    assertThat(AccessPoint.create(foo, AccessPointName.create("foo", "desc"))).isNotNull();
  }

  @Test
  public void shouldCreateMethodAccessWithinSuppliedType() {
    AccessPoint testee = AccessPoint.create(foo);
    AccessPoint actual = testee.methodAccess(AccessPointName.create("bar", "()V"));
    assertThat(foo).isEqualTo(actual.getElementName());
    assertThat(AccessPointName.create("bar", "()V")).isEqualTo(actual.getAttribute());
  }

}
