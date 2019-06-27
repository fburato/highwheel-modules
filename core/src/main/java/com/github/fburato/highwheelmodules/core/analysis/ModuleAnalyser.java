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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModuleAnalyser {

    private final ClassParser classParser;
    private final ClasspathRoot root;
    private final DefinitionVisitor definitionVisitor;

    public ModuleAnalyser(final ClassParser classParser, final ClasspathRoot root, Optional<Integer> evidenceLimit,
            ModuleGraphFactory factory) {
        this.classParser = classParser;
        this.root = root;
        this.definitionVisitor = new DefinitionVisitor(factory, evidenceLimit);
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
}
