package com.github.fburato.highwheelmodules.bytecodeparser.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

public class ClassLoaderClassPathRootTest {

    private ClassLoaderClassPathRoot testee;

    @BeforeEach
    public void setup() {
        this.testee = new ClassLoaderClassPathRoot(Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void shouldReturnNoClassNames() {

        assertThat(this.testee.classNames()).isEmpty();
    }

    @Test
    public void shouldReturnsBytesForClassesVisibleToParentLoader() throws Exception {
        assertThat(this.testee.getData(ElementName.fromClass(ClassLoaderClassPathRootTest.class))).isNotNull();
        assertThat(Test.class.getName()).isNotNull();
    }

    @Test
    public void testReturnsNullForClassesNotVisibleToParentLoader() throws Exception {
        assertThat(this.testee.getData(ElementName.fromString("FooFoo"))).isNull();
    }

    @Test
    public void testReturnsNullForResourcesNotVisibleToParentLoader() throws Exception {
        assertThat(this.testee.getResource("not defined")).isNull();
    }

    @Test
    public void testReturnsInputStreamForResourcesVisibleToParentLoader() throws Exception {
        assertThat(this.testee.getResource("aresource.txt")).isNotNull();
    }

}
