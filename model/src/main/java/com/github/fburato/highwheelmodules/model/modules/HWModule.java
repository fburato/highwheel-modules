package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.utils.GlobToRegex;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HWModule {

  public final String name;
  public final List<String> patternLiterals;
  private final List<Pattern> patterns;

  private HWModule(String name, Stream<String> patternLiteral) {
    this.name = name;
    this.patternLiterals = patternLiteral.collect(Collectors.toList());
    this.patterns = patternLiterals.stream().map(Pattern::compile).collect(Collectors.toList());
  }

  public static Optional<HWModule> make(String moduleName, String... globs) {
    return make(moduleName, Arrays.asList(globs));
  }

  public static Optional<HWModule> make(String moduleName, List<String> globs) {
    try {
      return Optional.of(new HWModule(moduleName, globs.stream().map(GlobToRegex::convertGlobToRegex)));
    } catch (PatternSyntaxException e) {
      return Optional.empty();
    }
  }

  public boolean contains(ElementName elementName) {
    return patterns.stream().anyMatch((p) -> p.matcher(elementName.asJavaName()).matches());
  }

  @Override
  public String toString() {
    return "HWModule{" +
        "name='" + name + '\'' +
        ", patternLiteral='" + patternLiterals + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    HWModule module = (HWModule) o;

    return Objects.equals(this.name, module.name) &&
        Objects.equals(this.patternLiterals, module.patternLiterals);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + patternLiterals.hashCode();
    return result;
  }
}
