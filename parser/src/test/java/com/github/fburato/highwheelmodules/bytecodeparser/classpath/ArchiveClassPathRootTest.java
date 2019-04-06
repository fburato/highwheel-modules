package com.github.fburato.highwheelmodules.bytecodeparser.classpath;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class ArchiveClassPathRootTest {

  private ArchiveClassPathRoot testee;

  @BeforeEach
  public void setup() throws Exception {
    // note mytests.jar is taken from
    // http://johanneslink.net/projects/cpsuite.jsp
    // assume GPL licence for this file. We do not link to any code within it
    // however
    this.testee = new ArchiveClassPathRoot(new File("mytests.jar"));
  }

  @Test
  public void classNamesShouldReturnAllClassNamesIArchive() {
    final Collection<ElementName> expected = Arrays.asList(
        ElementName.fromString("injar.p1.P1NoTest$InnerTest"),
        ElementName.fromString("injar.p1.P1NoTest"),
        ElementName.fromString("injar.p1.P1Test"),
        ElementName.fromString("injar.p2.P2Test"));
    assertThat(expected).isEqualTo(this.testee.classNames());
  }

  @Test
  public void getDataShouldReturnNullForUnknownClass() throws Exception {
    assertThat(this.testee.getData(ElementName.fromString("bar"))).isNull();
  }

  @Test
  public void getDataShouldReturnInputStreamForAKnownClass() throws Exception {
    assertThat(this.testee
        .getData(ElementName.fromString("injar.p1.P1Test"))).isNotNull();
  }

  @Test
  public void shouldReturnAReadableInputStream() {
    final byte b[] = new byte[100];
    try {
      final InputStream actual = this.testee.getData(ElementName
          .fromString("injar.p1.P1Test"));
      actual.read(b);
    } catch (final IOException ex) {
      fail("IO Exception should not be thrown");
    }
  }

}
