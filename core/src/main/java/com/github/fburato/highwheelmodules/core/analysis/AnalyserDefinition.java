package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.Collection;
import java.util.Objects;

public class AnalyserDefinition {
    public final Collection<HWModule> modules;
    public final Collection<Dependency> dependencies;
    public final Collection<NoStrictDependency> noStrictDependencies;

    public AnalyserDefinition(Collection<HWModule> modules, Collection<Dependency> dependencies,
            Collection<NoStrictDependency> noStrictDependencies) {
        this.modules = modules;
        this.dependencies = dependencies;
        this.noStrictDependencies = noStrictDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnalyserDefinition that = (AnalyserDefinition) o;
        return Objects.equals(modules, that.modules) && Objects.equals(dependencies, that.dependencies)
                && Objects.equals(noStrictDependencies, that.noStrictDependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modules, dependencies, noStrictDependencies);
    }

    @Override
    public String toString() {
        return "AnalyserDefinition{" + "modules=" + modules + ", dependencies=" + dependencies
                + ", noStrictDependencies=" + noStrictDependencies + '}';
    }
}
