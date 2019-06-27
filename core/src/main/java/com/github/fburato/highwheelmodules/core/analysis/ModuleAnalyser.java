package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.CompoundAccessVisitor;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraphFactory;
import com.github.fburato.highwheelmodules.utils.Pair;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.classpath.ClassParser;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModuleAnalyser {

    private final ClassParser classParser;
    private final ClasspathRoot root;
    private final Optional<Integer> evidenceLimit;
    private final ModuleGraphFactory factory;

    public ModuleAnalyser(final ClassParser classParser, final ClasspathRoot root, Optional<Integer> evidenceLimit,
            ModuleGraphFactory factory) {
        this.classParser = classParser;
        this.root = root;
        this.evidenceLimit = evidenceLimit;
        this.factory = factory;
    }

    public List<Pair<Definition, AnalyserModel.AnalysisResult>> analyseStrict(final List<Definition> definitions) {
        if (definitions.isEmpty()) {
            return new ArrayList<>();
        } else if (definitions.size() == 1) {
            return Collections.singletonList(Pair.make(definitions.get(0), internalAnalyseStrict(definitions.get(0))));
        } else {
            return internalAnalyseStrict(definitions);
        }
    }

    private List<Pair<Definition, AnalyserModel.AnalysisResult>> internalAnalyseStrict(
            final List<Definition> definitions) {
        final List<Pair<Definition, AnalysisState>> defVisitors = getAnalysisStates(definitions);
        final AccessVisitor visitor = new CompoundAccessVisitor(collectVisitors(defVisitors));
        try {
            classParser.parse(root, visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return defVisitors.stream().map(p -> Pair.make(p.first, StrictAnalyser.analyseStrict(p.first, p.second)))
                .collect(Collectors.toList());
    }

    private List<Pair<Definition, AnalysisState>> getAnalysisStates(List<Definition> definitions) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        return definitions.stream().map(def -> Pair.make(def, definitionVisitor.getAnalysisState(def)))
                .collect(Collectors.toList());
    }

    private List<AccessVisitor> collectVisitors(List<Pair<Definition, AnalysisState>> states) {
        return states.stream().map(p -> p.second.visitor).collect(Collectors.toList());
    }

    public AnalyserModel.AnalysisResult internalAnalyseStrict(final Definition definition) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        final AnalysisState analysisState = definitionVisitor.getAnalysisState(definition);

        try {
            classParser.parse(root, analysisState.visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return StrictAnalyser.analyseStrict(definition, analysisState);
    }

    public List<Pair<Definition, AnalyserModel.LooseAnalysisResult>> analyseLoose(final List<Definition> definitions) {
        if (definitions.isEmpty()) {
            return new ArrayList<>();
        } else if (definitions.size() == 1) {
            return Collections.singletonList(Pair.make(definitions.get(0), internalAnalyseLoose(definitions.get(0))));
        } else {
            return internalAnalyseLoose(definitions);
        }
    }

    private List<Pair<Definition, AnalyserModel.LooseAnalysisResult>> internalAnalyseLoose(
            final List<Definition> definitions) {
        final List<Pair<Definition, AnalysisState>> defVisitors = getAnalysisStates(definitions);
        final AccessVisitor visitor = new CompoundAccessVisitor(collectVisitors(defVisitors));

        try {
            classParser.parse(root, visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return defVisitors.stream().map(p -> Pair.make(p.first, LooseAnalyser.analyseLoose(p.first, p.second)))
                .collect(Collectors.toList());
    }

    private AnalyserModel.LooseAnalysisResult internalAnalyseLoose(final Definition definition) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        final AnalysisState analysisState = definitionVisitor.getAnalysisState(definition);

        try {
            classParser.parse(root, analysisState.visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return LooseAnalyser.analyseLoose(definition, analysisState);
    }
}
