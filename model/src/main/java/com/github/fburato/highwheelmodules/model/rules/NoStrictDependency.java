package com.github.fburato.highwheelmodules.model.rules;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import org.immutables.value.Value.Immutable;

@Immutable
public interface NoStrictDependency extends Rule {

    static NoStrictDependency make(final HWModule source, final HWModule dest) {
        return ImmutableNoStrictDependency.builder().source(source).dest(dest).build();
    }

    HWModule source();

    HWModule dest();
}
