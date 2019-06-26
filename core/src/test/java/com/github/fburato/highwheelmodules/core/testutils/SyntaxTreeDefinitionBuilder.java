package com.github.fburato.highwheelmodules.core.testutils;

import com.github.fburato.highwheelmodules.core.specification.SyntaxTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SyntaxTreeDefinitionBuilder extends Builder<SyntaxTree.Definition, SyntaxTreeDefinitionBuilder> {

    public Optional<String> prefix;
    public Optional<List<String>> whiteList;
    public Optional<List<String>> blackList;
    public Optional<String> mode;
    public List<SyntaxTree.ModuleDefinition> moduleDefinitions;
    public List<SyntaxTree.Rule> rules;

    private SyntaxTreeDefinitionBuilder(ArrayList<Consumer<SyntaxTreeDefinitionBuilder>> consumers) {
        super(consumers);
    }

    @Override
    protected SyntaxTreeDefinitionBuilder copy() {
        return new SyntaxTreeDefinitionBuilder(new ArrayList<>(buildSequence));
    }

    @Override
    protected SyntaxTree.Definition makeValue() {
        return new SyntaxTree.Definition(prefix, whiteList, blackList, mode, moduleDefinitions, rules);
    }

    public static SyntaxTreeDefinitionBuilder baseBuilder() {
        return new SyntaxTreeDefinitionBuilder(new ArrayList<>()).with($ -> {
            $.prefix = Optional.empty();
            $.whiteList = Optional.empty();
            $.blackList = Optional.empty();
            $.mode = Optional.empty();
            $.moduleDefinitions = new ArrayList<>();
            $.rules = new ArrayList<>();
        });
    }
}
