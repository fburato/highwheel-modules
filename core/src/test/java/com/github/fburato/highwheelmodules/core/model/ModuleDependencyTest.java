package com.github.fburato.highwheelmodules.core.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDependencyTest {

  private final HWModule moduleA = HWModule.make("module A", "module A").get();
  private final HWModule moduleB = HWModule.make("module B", "module B").get();
  private final HWModule moduleBAlt = HWModule.make("module B", "module B").get();

  @Test
  public void getCountShouldReturnZeroOnNewDepedency() {
    assertThat(new ModuleDependency(moduleA, moduleB).getCount()).isEqualTo(0);
  }

  @Test
  public void getCountShouldIncreaseWhenCallingIncrementCount() {
    final ModuleDependency testee = new ModuleDependency(moduleA, moduleB);

    assertThat(testee.getCount()).isEqualTo(0);
    testee.incrementCount();
    assertThat(testee.getCount()).isEqualTo(1);
    assertThat(testee.getCount()).isEqualTo(1);
    testee.incrementCount();
    assertThat(testee.getCount()).isEqualTo(2);
  }

  @Test
  public void equalsShouldWorkOnDeepComparison() {
    final ModuleDependency testee = new ModuleDependency(moduleA, moduleB);
    final ModuleDependency alternative = new ModuleDependency(moduleA, moduleBAlt);

    assertThat(testee).isEqualTo(alternative);
  }

  @Test
  public void equalsShouldWorkOnComparisonWithNull() {
    final ModuleDependency testee = new ModuleDependency(moduleA, moduleB);

    assertThat(testee).isNotEqualTo(null);
  }

  @Test
  public void equalsShouldWorkOnComparisonWithOtherType() {
    final ModuleDependency testee = new ModuleDependency(moduleA, moduleB);

    assertThat(testee.equals(new Object())).isFalse();
  }
}
