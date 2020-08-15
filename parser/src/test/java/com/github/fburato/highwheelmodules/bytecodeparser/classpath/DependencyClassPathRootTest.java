package com.github.fburato.highwheelmodules.bytecodeparser.classpath;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependencyClassPathRootTest {

    private final ClasspathRoot child = mock(ClasspathRoot.class);

    private final DependencyClassPathRoot testee = new DependencyClassPathRoot(this.child);

    @Test
    public void shouldNotReturnClassNames() {
        when(this.child.classNames()).thenReturn(Collections.singletonList(ElementName.fromString("foo")));
        assertThat(this.testee.classNames()).isEmpty();
    }

    @Test
    public void shouldReturnResourcesFromChild() throws IOException {
        final InputStream is = mock(InputStream.class);
        when(this.child.getResource("foo")).thenReturn(is);
        assertThat(is).isSameAs(this.testee.getResource("foo"));
    }

    @Test
    public void shouldReturnDataFromChild() throws IOException {
        final ElementName cn = ElementName.fromString("foo");
        final InputStream is = mock(InputStream.class);
        when(this.child.getData(cn)).thenReturn(is);
        assertThat(is).isSameAs(this.testee.getData(cn));
    }

}
