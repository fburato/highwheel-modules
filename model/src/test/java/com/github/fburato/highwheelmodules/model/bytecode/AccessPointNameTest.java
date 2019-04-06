package com.github.fburato.highwheelmodules.model.bytecode;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessPointNameTest {

    @Test
    public void shouldObeyHashcodeEqualsContract() {
        EqualsVerifier.forClass(AccessPointName.class).verify();
    }

    @Test
    public void shouldReplaceAngleBracketsInAttributes() {
        assertThat(AccessPointName.create("(init)", "")).isEqualTo(AccessPointName.create("<init>", ""));
    }

}
