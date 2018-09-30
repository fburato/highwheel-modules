package com.github.fburato.highwheelmodules.utils;

import org.junit.Test;

import java.util.regex.Pattern;


import static org.assertj.core.api.Assertions.*;

public class GlobToRegexTest {

  @Test
  public void shouldFindExactMatches() {
    final String value = "org.foo.foo";
    assertThat(matches(value, value)).isTrue();
  }

  @Test
  public void shouldNotMatchNonMatchingStringWhenNoWildcardsPresent() {
    final String value = "org.foo.foo";
    final String glob = "org.foo";
    assertThat(matches(glob, value)).isFalse();
  }

  @Test
  public void shouldMatchEverythingAfterAStar() {
    final String glob = "org.foo.*";
    assertThat(matches(glob, "org.foo.foo")).isTrue();
    assertThat(matches(glob, "org.foo.")).isTrue();
    assertThat(matches(glob, "org.foo.bar")).isTrue();
  }

  @Test
  public void shouldNotMatchIfContentDiffersBeforeAStar() {
    final String glob = new String("org.foo.*");
    assertThat(matches(glob, "org.fo")).isFalse();
  }

  @Test
  public void shouldEscapeDotsInGeneratedRegex() {
    final String glob = new String("org.foo.bar");
    assertThat(matches(glob, "orgafooabar")).isFalse();
  }

  @Test
  public void shouldSupportQuestionMarkWildCard() {
    final String glob = new String("org?foo?bar");
    assertThat(matches(glob, "org.foo.bar")).isTrue();
    assertThat(matches(glob, "orgafooabar")).isTrue();
  }

  @Test
  public void shouldEscapeEscapesInGeneratedRegex() {
    final String glob = new String("org.\\bar");
    assertThat(matches(glob, "org.\\bar")).isTrue();
    assertThat(matches(glob, "org.bar")).isFalse();
  }

  @Test
  public void shouldEscapeDollarSign() {
    final String glob = new String("org$bar");
    assertThat(matches(glob, "org$bar")).isTrue();
  }

  @Test
  public void shouldSupportMultipleWildcards() {
    final String glob = new String("foo*bar*car");
    assertThat(matches(glob, "foo!!!bar!!!car")).isTrue();
    assertThat(matches(glob, "foo!!!!!car")).isFalse();
  }

  @Test
  public void shouldBeCaseSensitice() {
    final String glob = new String("foo*bar*car");
    assertThat(matches(glob, "foo!!!bar!!!car")).isTrue();
    assertThat(matches(glob, "foo!!!Bar!!!car")).isFalse();
  }

  private boolean matches(final String String, final String value) {
    return Pattern.matches(GlobToRegex.convertGlobToRegex(String), value);
  }
}
