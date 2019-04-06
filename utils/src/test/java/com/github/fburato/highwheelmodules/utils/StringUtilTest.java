package com.github.fburato.highwheelmodules.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;
import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilTest {

  private static class TestClass {
    public final int a;
    public final String b;

    public TestClass(int a, String b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return b + "-" + a;
    }
  }

  @Test
  public void joinShouldReturnEmptyStringIfIterableIsEmpty() {
    assertThat(join(",", new ArrayList<String>())).isEqualTo("");
  }

  @Test
  public void joinShouldReturnToStringOfContainedObjectIfOnlyOneAvailable() {
    assertThat(join(",", Arrays.asList(new TestClass(9, "hello")))).isEqualTo("hello-9");
  }

  @Test
  public void joinShouldReturnTheExpectedSeparatedStringWithManyElements() {
    assertThat(join(";", Arrays.asList(new TestClass(10, "foo"), new TestClass(41, "bar")))).isEqualTo("foo-10;bar-41");
  }
}
