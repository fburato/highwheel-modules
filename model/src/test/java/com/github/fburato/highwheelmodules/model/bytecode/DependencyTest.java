package com.github.fburato.highwheelmodules.model.bytecode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyTest {

    @Test
    public void shouldTreatNonSameInstacesAsNonEqual() {
        Dependency a = new Dependency();
        Dependency b = new Dependency();
        assertThat(a.equals(b)).isFalse();
        assertThat(b.equals(a)).isFalse();
    }

    @Test
    public void shouldSumStrengthsOfConstituents() {
        Dependency testee = new Dependency();
        AccessPoint source = AccessPoint.create(ElementName.fromString("foo"));
        AccessPoint dest = AccessPoint.create(ElementName.fromString("foo"));
        testee.addDependency(source, dest, AccessType.COMPOSED);
        testee.addDependency(source, dest, AccessType.USES);
        assertThat(AccessType.COMPOSED.getStrength() + AccessType.USES.getStrength()).isEqualTo(testee.getStrength());
    }

}
