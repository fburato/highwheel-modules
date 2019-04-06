package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.Collection;

public class Definition {
    public final Collection<HWModule> modules;
    public final Collection<Dependency> dependencies;
    public final Collection<NoStrictDependency> noStrictDependencies;

    public Definition(Collection<HWModule> modules, Collection<Dependency> dependencies,
            Collection<NoStrictDependency> noStrictDependencies) {
        this.modules = modules;
        this.dependencies = dependencies;
        this.noStrictDependencies = noStrictDependencies;
    }

    @Override
    public String toString() {
        return "Definition{" + "modules=" + modules + ", dependencies=" + dependencies + ", noStrictDependencies="
                + noStrictDependencies + '}';
    }
}