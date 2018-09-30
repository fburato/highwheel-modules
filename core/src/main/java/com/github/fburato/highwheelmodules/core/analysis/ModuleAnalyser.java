package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.model.*;
import org.pitest.highwheel.classpath.ClassParser;
import org.pitest.highwheel.classpath.ClasspathRoot;

import java.io.IOException;
import java.util.Optional;

public class ModuleAnalyser {

  private final ClassParser classParser;
  private final ClasspathRoot root;
  private final Optional<Integer> evidenceLimit;

  public ModuleAnalyser(final ClassParser classParser, final ClasspathRoot root, Optional<Integer> evidenceLimit) {
    this.classParser = classParser;
    this.root = root;
    this.evidenceLimit = evidenceLimit;
  }

  public AnalyserModel.StrictAnalysisResult analyseStrict(final Definition definition) {
    final DefinitionVisitor definitionVisitor = new DefinitionVisitor(evidenceLimit);
    final AnalysisState analysisState = definitionVisitor.getAnalysisState(definition);

    try {
      classParser.parse(root, analysisState.visitor);
    } catch (IOException e) {
      throw new AnalyserException(e);
    }

    return StrictAnalyser.analyseStrict(definition,analysisState);
  }

  public AnalyserModel.LooseAnalysisResult analyseLoose(final Definition definition) {
    final DefinitionVisitor definitionVisitor = new DefinitionVisitor(evidenceLimit);
    final AnalysisState analysisState = definitionVisitor.getAnalysisState(definition);

    try {
      classParser.parse(root, analysisState.visitor);
    } catch (IOException e) {
      throw new AnalyserException(e);
    }

    return LooseAnalyser.analyseLoose(definition,analysisState);
  }
}
