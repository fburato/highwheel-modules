package com.github.fburato.highwheelmodules.model.bytecode;


import java.util.Arrays;
import java.util.List;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ElementNameTest {

    @Test
    public void shouldObeyHashcodeEqualsContract() {
        EqualsVerifier.forClass(ElementName.class).verify();
    }

    @Test
    public void shouldConvertJavaNamesToInternalNames() {
        final ElementName testee = new ElementName("com.foo.bar");
        assertThat("com/foo/bar").isEqualTo(testee.asInternalName());
    }

    @Test
    public void shouldConvertInternalNamesToJavaNames() {
        final ElementName testee = new ElementName("com/foo/bar");
        assertThat("com.foo.bar").isEqualTo(testee.asJavaName());
    }

    @Test
    public void shouldTreatSameClassNameAsEqual() {
        final ElementName left = new ElementName("com/foo/bar");
        final ElementName right = new ElementName("com.foo.bar");
        assertThat(left.equals(right)).isTrue();
        assertThat(right.equals(left)).isTrue();
    }

    @Test
    public void shouldDisplayJavaNameInToString() {
        final ElementName testee = new ElementName("com/foo/bar");
        assertThat("com.foo.bar").isEqualTo(testee.toString());
    }

    @Test
    public void getNameWithoutPackageShouldReturnNameOnlyWhenClassIsOuterClass() {
        assertThat(new ElementName("String")).isEqualTo(
                new ElementName(String.class).getNameWithoutPackage());
    }

    static class Foo {

    }

    @Test
    public void getNameWithoutPackageShouldReturnNameWhenClassIsInnerClass() {
        assertThat(new ElementName("ElementNameTest$Foo")).isEqualTo(new ElementName(
                Foo.class).getNameWithoutPackage());
    }

    @Test
    public void getNameWithoutPackageShouldReturnNameWhenClassInPackageDefault() {
        assertThat(new ElementName("Foo")).isEqualTo(
                new ElementName("Foo").getNameWithoutPackage());
    }

    @Test
    public void getPackageShouldReturnEmptyPackageWhenClassInPackageDefault() {
        assertThat(new ElementName("")).isEqualTo(new ElementName("Foo").getParent());
    }

    @Test
    public void getPackageShouldReturnPackageWhenClassWithinAPackage() {
        assertThat(new ElementName("com.github.fburato.highwheelmodules.model.bytecode")).isEqualTo(
                new ElementName(ElementNameTest.class).getParent());
    }

    @Test
    public void withoutSuffixCharsShouldReturnPacakgeAndClassWithoutSuffixChars() {
        assertThat(new ElementName("com.example.Foo")).isEqualTo(new ElementName(
                "com.example.FooTest").withoutSuffixChars(4));
    }

    @Test
    public void withoutPrefeixCharsShouldReturnPacakgeAndClassWithoutPrefixChars() {
        assertThat(new ElementName("com.example.Foo")).isEqualTo(new ElementName(
                "com.example.TestFoo").withoutPrefixChars(4));
    }

    @Test
    public void shouldSortByName() {
        final ElementName a = ElementName.fromString("a.a.c");
        final ElementName b = ElementName.fromString("a.b.c");
        final ElementName c = ElementName.fromString("b.a.c");

        final List<ElementName> actual = Arrays.asList(b, c, a);
        assertThat(actual).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    public void shouldProduceSameHashCodeForSameClass() {
        assertThat(ElementName.fromString("org/example/Foo").hashCode()).isEqualTo(
                ElementName.fromString("org.example.Foo").hashCode());
    }

    @Test
    public void shouldProduceDifferentHashCodeForDifferentClasses() {
        assertThat(ElementName.fromString("org/example/Foo").hashCode())
                .isNotEqualTo(ElementName
                        .fromString("org.example.Bar").hashCode());
    }

    @Test
    public void shouldTreatSameClassAsEqual() {
        assertThat(ElementName.fromString("org/example/Foo")).isEqualTo(
                ElementName.fromString("org.example.Foo"));
    }

    @Test
    public void shouldTreatDifferentClassesAsNotEqual() {
        assertThat(ElementName.fromString("org/example/Foo")).isNotEqualTo(
                ElementName.fromString("org.example.Bar"));
    }

}
