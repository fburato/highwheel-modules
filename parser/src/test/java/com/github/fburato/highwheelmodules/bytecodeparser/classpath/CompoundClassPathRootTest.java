package com.github.fburato.highwheelmodules.bytecodeparser.classpath;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompoundClassPathRootTest {

    private final ClasspathRoot child1 = mock(ClasspathRoot.class);

    private final ClasspathRoot child2 = mock(ClasspathRoot.class);

    private final CompoundClassPathRoot testee = new CompoundClassPathRoot(Arrays.asList(this.child1, this.child2));

    @Test
    public void shouldReturnNamesOfAllClassesKnownByChildren() {
        final ElementName foo = ElementName.fromString("Foo");
        final ElementName bar = ElementName.fromString("Foo");

        when(this.child1.classNames()).thenReturn(Collections.singletonList(foo));
        when(this.child2.classNames()).thenReturn(Collections.singletonList(bar));

        assertThat(this.testee.classNames()).containsExactly(foo, bar);

    }

    @Test
    public void shouldReturnNullWhenNoChildCanSupplyData() throws IOException {
        assertThat(this.testee.getData(ElementName.fromString("unknown"))).isNull();
    }

    @Test
    public void shouldReturnNullWhenNoChildCanSupplyResource() throws IOException {
        assertThat(this.testee.getResource("unknown")).isNull();
    }

    @Test
    public void shouldReturnClassDataFromChildren() throws IOException {
        when(this.child1.getData(any(ElementName.class))).thenReturn(null);
        final InputStream is = mock(InputStream.class);
        when(this.child1.getData(any(ElementName.class))).thenReturn(is);
        assertThat(this.testee.getData(ElementName.fromString("Foo"))).isSameAs(is);
    }

    @Test
    public void shouldReturnResourcesFromChildren() throws IOException {
        when(this.child1.getResource(any(String.class))).thenReturn(null);
        final InputStream is = mock(InputStream.class);
        when(this.child1.getResource(any(String.class))).thenReturn(is);
        assertThat(this.testee.getResource("Foo")).isSameAs(is);
    }

}
