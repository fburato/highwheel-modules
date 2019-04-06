package com.github.fburato.highwheelmodules.bytecodeparser;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class ClassPathParserTest {

    private ClassPathParser testee;

    @Mock
    private ClasspathRoot cp;

    @Mock
    private Predicate<ElementName> filter;

    @Mock
    private AccessVisitor v;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.testee = new ClassPathParser(this.filter);
    }

    @Test
    public void shouldNotParseClassesThatDoNotMatchFilter() throws IOException {
        final ElementName foo = ElementName.fromString("foo");
        when(this.cp.classNames()).thenReturn(Collections.singleton(foo));
        when(this.filter.test(foo)).thenReturn(false);
        testee.parse(cp, v);
        verify(cp, never()).getData(foo);
    }

    @Test
    public void shouldCloseClassInputStreams() throws IOException {
        final ElementName foo = ElementName.fromString("foo");
        when(this.cp.classNames()).thenReturn(Collections.singleton(foo));
        when(this.filter.test(foo)).thenReturn(true);
        final InputStream is = Mockito.mock(InputStream.class);
        when(this.cp.getData(foo)).thenReturn(is);
        doThrow(new IOException()).when(is).read();
        doThrow(new IOException()).when(is).read(any(), anyInt(), anyInt());
        try {
            this.testee.parse(cp, this.v);
        } catch (final IOException ex) {
            // expected
        }
        verify(is).close();

    }

}
