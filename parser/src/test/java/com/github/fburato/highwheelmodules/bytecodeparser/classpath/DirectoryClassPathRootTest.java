package com.github.fburato.highwheelmodules.bytecodeparser.classpath;

import java.io.File;

import org.junit.jupiter.api.Test;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryClassPathRootTest {

    private DirectoryClassPathRoot testee;

    @Test
    public void getDataShouldReturnNullForUnknownClass() throws Exception {
        this.testee = new DirectoryClassPathRoot(new File("foo"));
        assertThat(this.testee.getData(ElementName.fromString("bar"))).isNull();
    }

    @Test
    public void shouldReturnClassNames() {
        final File root = new File("target/test-classes/"); // this is going to be
        // flakey as hell
        this.testee = new DirectoryClassPathRoot(root);
        assertThat(
                this.testee.classNames().contains(ElementName.fromString(DirectoryClassPathRootTest.class.getName())))
                        .isTrue();
    }

}
