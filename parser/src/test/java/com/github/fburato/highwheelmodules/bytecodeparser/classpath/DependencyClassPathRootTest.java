package com.github.fburato.highwheelmodules.bytecodeparser.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class DependencyClassPathRootTest {

  private DependencyClassPathRoot testee;

  @Mock
  private ClasspathRoot           child;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.testee = new DependencyClassPathRoot(this.child);
  }

  @Test
  public void shouldNotReturnClassNames() {
    when(this.child.classNames()).thenReturn(
        Arrays.asList(ElementName.fromString("foo")));
    assertThat(this.testee.classNames()).isEmpty();
  }

  @Test
  public void shouldReturnResourcesFromChild() throws IOException {
    final InputStream is = Mockito.mock(InputStream.class);
    when(this.child.getResource("foo")).thenReturn(is);
    assertThat(is).isSameAs(this.testee.getResource("foo"));
  }

  @Test
  public void shouldReturnDataFromChild() throws IOException {
    final ElementName cn = ElementName.fromString("foo");
    final InputStream is = Mockito.mock(InputStream.class);
    when(this.child.getData(cn)).thenReturn(is);
    assertThat(is).isSameAs(this.testee.getData(cn));
  }

}
