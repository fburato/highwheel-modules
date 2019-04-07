package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.utils.GlobToRegex;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AnonymousModule implements MatchingModule {

    public final List<String> patternLiterals;
    final List<Pattern> patterns;

    AnonymousModule(Stream<String> patternLiteral) {
        this.patternLiterals = patternLiteral.collect(Collectors.toList());
        this.patterns = patternLiterals.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    public static Optional<AnonymousModule> make(String... globs) {
        return make(Arrays.asList(globs));
    }

    public static Optional<AnonymousModule> make(List<String> globs) {
        try {
            return Optional.of(new AnonymousModule(globs.stream().map(GlobToRegex::convertGlobToRegex)));
        } catch (PatternSyntaxException e) {
            return Optional.empty();
        }
    }

    public boolean contains(ElementName elementName) {
        return patterns.stream().anyMatch(p -> p.matcher(elementName.asJavaName()).matches());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final AnonymousModule that = (AnonymousModule) o;
        final List<String> thisCopy = new ArrayList<>(patternLiterals);
        final List<String> thatCopy = new ArrayList<>(that.patternLiterals);
        thisCopy.sort(String::compareTo);
        thatCopy.sort(String::compareTo);
        return Objects.equals(thisCopy, thatCopy);
    }

    @Override
    public int hashCode() {
        final List<String> thisCopy = new ArrayList<>(patternLiterals);
        thisCopy.sort(String::compareTo);
        return Objects.hash(thisCopy);
    }
}
