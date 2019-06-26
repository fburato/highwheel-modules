package com.github.fburato.highwheelmodules.core.testutils;

import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode;
import com.github.fburato.highwheelmodules.model.modules.AnonymousModule;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public class DefinitionBuilder extends Builder<Definition, DefinitionBuilder> {

    public Optional<AnonymousModule> whitelist;
    public Optional<AnonymousModule> blackList;
    public AnalysisMode mode;
    public Collection<HWModule> modules;
    public Collection<Dependency> dependencies;
    public Collection<NoStrictDependency> noStrictDependencies;

    private DefinitionBuilder(ArrayList<Consumer<DefinitionBuilder>> consumers) {
        super(consumers);
    }

    @Override
    protected DefinitionBuilder copy() {
        return new DefinitionBuilder(new ArrayList<>(this.buildSequence));
    }

    @Override
    protected Definition makeValue() {
        return new Definition(whitelist, blackList, mode, modules, dependencies, noStrictDependencies);
    }

    public static DefinitionBuilder baseBuilder() {
        return new DefinitionBuilder(new ArrayList<>()).with($ -> {
            $.whitelist = Optional.empty();
            $.blackList = Optional.empty();
            $.modules = new ArrayList<>();
            $.dependencies = new ArrayList<>();
            $.noStrictDependencies = new ArrayList<>();
        });
    }
}
