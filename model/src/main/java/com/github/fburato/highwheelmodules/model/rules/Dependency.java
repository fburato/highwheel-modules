package com.github.fburato.highwheelmodules.model.rules;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import org.immutables.value.Value.Immutable;

@Immutable
public interface Dependency extends Rule {

    static Dependency make(final HWModule source, final HWModule dest) {
        return ImmutableDependency.builder().source(source).dest(dest).build();
    }

    HWModule source();

    HWModule dest();
}
