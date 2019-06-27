package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.algorithms.CompoundAccessVisitor;
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode;
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
import java.util.function.Supplier;
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

    public List<AnalyserModel.AnalysisResult> analyse(final List<Definition> definitions) {
        if (definitions.isEmpty()) {
            return new ArrayList<>();
        } else {
            final Pair<AccessVisitor, List<Supplier<AnalyserModel.AnalysisResult>>> visitorAndAnalysis = visitorAndAnalysis(
                    definitions);
            try {
                classParser.parse(root, visitorAndAnalysis.first);
            } catch (IOException e) {
                throw new AnalyserException(e);
            }
            return visitorAndAnalysis.second.stream().map(Supplier::get).collect(Collectors.toList());
        }
    }

    private Pair<AccessVisitor, List<Supplier<AnalyserModel.AnalysisResult>>> visitorAndAnalysis(
            final List<Definition> definitions) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        final List<AccessVisitor> visitors = new ArrayList<>();
        final LooseAnalyser looseAnalyser = new LooseAnalyser();
        final StrictAnalyser strictAnalyser = new StrictAnalyser();
        final List<Supplier<AnalyserModel.AnalysisResult>> processors = definitions
                .stream().<Supplier<AnalyserModel.AnalysisResult>> map(definition -> {
                    final AnalysisState state = definitionVisitor.getAnalysisState(definition);
                    visitors.add(state.visitor);
                    if (definition.mode == AnalysisMode.STRICT) {
                        return () -> strictAnalyser.analyse(state);
                    } else {
                        return () -> looseAnalyser.analyse(state);
                    }
                }).collect(Collectors.toList());
        return Pair.make(new CompoundAccessVisitor(visitors), processors);
    }

    public List<AnalyserModel.AnalysisResult> analyseStrict(final List<Definition> definitions) {
        if (definitions.isEmpty()) {
            return new ArrayList<>();
        } else if (definitions.size() == 1) {
            return Collections.singletonList(internalAnalyseStrict(definitions.get(0)));
        } else {
            return internalAnalyseStrict(definitions);
        }
    }

    private List<AnalyserModel.AnalysisResult> internalAnalyseStrict(final List<Definition> definitions) {
        final List<AnalysisState> analysisStates = getAnalysisStates(definitions);
        final AccessVisitor visitor = new CompoundAccessVisitor(collectVisitors(analysisStates));
        try {
            classParser.parse(root, visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return analysisStates.stream().map(StrictAnalyser::analyseStrict).collect(Collectors.toList());
    }

    private List<AnalysisState> getAnalysisStates(List<Definition> definitions) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        return definitions.stream().map(definitionVisitor::getAnalysisState).collect(Collectors.toList());
    }

    private List<AccessVisitor> collectVisitors(List<AnalysisState> states) {
        return states.stream().map(p -> p.visitor).collect(Collectors.toList());
    }

    public AnalyserModel.AnalysisResult internalAnalyseStrict(final Definition definition) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        final AnalysisState analysisState = definitionVisitor.getAnalysisState(definition);

        try {
            classParser.parse(root, analysisState.visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return StrictAnalyser.analyseStrict(analysisState);
    }

    public List<AnalyserModel.AnalysisResult> analyseLoose(final List<Definition> definitions) {
        if (definitions.isEmpty()) {
            return new ArrayList<>();
        } else if (definitions.size() == 1) {
            return Collections.singletonList(internalAnalyseLoose(definitions.get(0)));
        } else {
            return internalAnalyseLoose(definitions);
        }
    }

    private List<AnalyserModel.AnalysisResult> internalAnalyseLoose(final List<Definition> definitions) {
        final List<AnalysisState> analysisStates = getAnalysisStates(definitions);
        final AccessVisitor visitor = new CompoundAccessVisitor(collectVisitors(analysisStates));

        try {
            classParser.parse(root, visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return analysisStates.stream().map(LooseAnalyser::analyseLoose).collect(Collectors.toList());
    }

    private AnalyserModel.AnalysisResult internalAnalyseLoose(final Definition definition) {
        final DefinitionVisitor definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
        final AnalysisState analysisState = definitionVisitor.getAnalysisState(definition);

        try {
            classParser.parse(root, analysisState.visitor);
        } catch (IOException e) {
            throw new AnalyserException(e);
        }

        return LooseAnalyser.analyseLoose(analysisState);
    }
}
