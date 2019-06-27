package com.github.fburato.highwheelmodules.model.modules;

import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;
import com.github.fburato.highwheelmodules.utils.Builder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class Definition {
    public final Optional<AnonymousModule> whitelist;
    public final Optional<AnonymousModule> blackList;
    public final AnalysisMode mode;
    public final Collection<HWModule> modules;
    public final Collection<Dependency> dependencies;
    public final Collection<NoStrictDependency> noStrictDependencies;

    public Definition(Collection<HWModule> modules, Collection<Dependency> dependencies,
            Collection<NoStrictDependency> noStrictDependencies) {
        this(Optional.empty(), Optional.empty(), AnalysisMode.STRICT, modules, dependencies, noStrictDependencies);
    }

    public Definition(Optional<AnonymousModule> whitelist, Optional<AnonymousModule> blackList,
            Collection<HWModule> modules, Collection<Dependency> dependencies,
            Collection<NoStrictDependency> noStrictDependencies) {
        this(whitelist, blackList, AnalysisMode.STRICT, modules, dependencies, noStrictDependencies);
    }

    public Definition(Optional<AnonymousModule> whitelist, Optional<AnonymousModule> blackList, AnalysisMode mode,
            Collection<HWModule> modules, Collection<Dependency> dependencies,
            Collection<NoStrictDependency> noStrictDependencies) {
        this.whitelist = whitelist;
        this.blackList = blackList;
        this.mode = mode;
        this.modules = modules;
        this.dependencies = dependencies;
        this.noStrictDependencies = noStrictDependencies;
    }

    @Override
    public String toString() {
        return "Definition{" + "modules=" + modules + ", dependencies=" + dependencies + ", noStrictDependencies="
                + noStrictDependencies + '}';
    }

    public static class DefinitionBuilder extends Builder<Definition, DefinitionBuilder> {

        public Optional<AnonymousModule> whitelist;
        public Optional<AnonymousModule> blackList;
        public AnalysisMode mode;
        public Collection<HWModule> modules;
        public Collection<Dependency> dependencies;
        public Collection<NoStrictDependency> noStrictDependencies;

        private DefinitionBuilder() {
            super(DefinitionBuilder::new);
        }

        @Override
        protected Definition makeValue() {
            return new Definition(whitelist, blackList, mode, modules, dependencies, noStrictDependencies);
        }

        public static DefinitionBuilder baseBuilder() {
            return new DefinitionBuilder().with($ -> {
                $.whitelist = Optional.empty();
                $.blackList = Optional.empty();
                $.mode = AnalysisMode.STRICT;
                $.modules = Collections.emptyList();
                $.dependencies = Collections.emptyList();
                $.noStrictDependencies = Collections.emptyList();
            });
        }
    }
}