package com.github.fburato.highwheelmodules.model.modules;

import org.junit.jupiter.api.Test;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import static org.assertj.core.api.Assertions.assertThat;

public class HWModuleTest {
  public static final String MODULE_NAME = "module name";
  public static final String GLOB = "org.pitest.foo*";
  private final HWModule testee = HWModule.make(MODULE_NAME, GLOB).get();

  @Test
  public void makeShouldFailIfRegexPassedIsInvalid() {
    assertThat(HWModule.make("a module", "[asdf").isPresent()).isFalse();
  }

  @Test
  public void makeShouldNotFailIfRegexPassedIsValid() {
    assertThat(HWModule.make("another module", ".*").isPresent()).isTrue();
  }

  @Test
  public void containsShouldBeTrueIfElementNameMatchesPattern() {
    assertThat(testee.contains(new ElementName("org.pitest.foo.Something"))).isTrue();
  }

  @Test
  public void containsShouldBeTrueOnMultiPatternModule() {
    HWModule testee = HWModule.make("a module with two patterns", "a*", "b*").get();
    assertThat(testee.contains(new ElementName("afoo"))).isTrue();
    assertThat(testee.contains(new ElementName("bfoo"))).isTrue();
  }

  @Test
  public void containsShouldFailIfAnyPatternFails() {
    assertThat(HWModule.make("a module", "valid", "[invalid").isPresent()).isFalse();
  }

  @Test
  public void containsShouldBeFalseIfElementNameDoesNotMatchPattern() {
    assertThat(testee.contains(new ElementName("not.pitest.foo"))).isFalse();
  }

  @Test
  public void equalsShouldWorkOnModuleNameAndGlob() {
    assertThat(testee).isEqualTo(HWModule.make(MODULE_NAME, GLOB).get());
  }
}
