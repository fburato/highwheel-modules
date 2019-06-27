package com.github.fburato.highwheelmodules.core;

import com.github.fburato.highwheelmodules.core.analysis.AnalyserException;
import com.github.fburato.highwheelmodules.core.analysis.AnalyserModel;
import com.github.fburato.highwheelmodules.core.analysis.ModuleAnalyser;
import com.github.fburato.highwheelmodules.core.externaladapters.GuavaGraphFactory;
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.core.specification.Compiler;
import com.github.fburato.highwheelmodules.core.specification.SyntaxTree;
import com.github.fburato.highwheelmodules.core.specification.parsers.DefinitionParser;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraphFactory;
import com.github.fburato.highwheelmodules.utils.Pair;
import com.github.fburato.highwheelmodules.bytecodeparser.ClassPathParser;
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.ArchiveClassPathRoot;
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.CompoundClassPathRoot;
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.DirectoryClassPathRoot;
import com.github.fburato.highwheelmodules.model.classpath.ClassParser;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AnalyserFacade {

    private static final Predicate<ElementName> includeAll = (item) -> true;

    public interface Printer {
        void info(String msg);
    }

    public interface EventSink {
        interface PathEventSink {
            void ignoredPaths(List<String> ignored);

            void directories(List<String> directories);

            void jars(List<String> jars);
        }

        interface MeasureEventSink {
            void fanInOutMeasure(String module, int fanIn, int fanOut);
        }

        interface StrictAnalysisEventSink {
            void dependenciesCorrect();

            void directDependenciesCorrect();

            void dependencyViolationsPresent();

            void dependencyViolation(String sourceModule, String destModule, List<String> expectedPath,
                    List<String> actualPath, List<List<Pair<String, String>>> evidences);

            void noDirectDependenciesViolationPresent();

            void noDirectDependencyViolation(String sourceModule, String destModule);
        }

        interface LooseAnalysisEventSink {

            void allDependenciesPresent();

            void noUndesiredDependencies();

            void absentDependencyViolationsPresent();

            void absentDependencyViolation(String sourceModule, String destModule);

            void undesiredDependencyViolationsPresent();

            void undesiredDependencyViolation(String sourceModule, String destModule, List<String> path,
                    List<List<Pair<String, String>>> evidences);
        }
    }

    private final Printer printer;
    private final EventSink.PathEventSink pathEventSink;
    private final EventSink.MeasureEventSink measureEventSink;
    private final EventSink.StrictAnalysisEventSink strictAnalysisEventSink;
    private final EventSink.LooseAnalysisEventSink looseAnalysisEventSink;
    private final ModuleGraphFactory factory = new GuavaGraphFactory();

    public AnalyserFacade(final Printer printer, final EventSink.PathEventSink pathEventSink,
            final EventSink.MeasureEventSink measureEventSink,
            final EventSink.StrictAnalysisEventSink strictAnalysisEventSink,
            final EventSink.LooseAnalysisEventSink looseAnalysisEventSink) {
        this.printer = printer;
        this.pathEventSink = pathEventSink;
        this.measureEventSink = measureEventSink;
        this.strictAnalysisEventSink = strictAnalysisEventSink;
        this.looseAnalysisEventSink = looseAnalysisEventSink;
    }

    public void runAnalysis(final List<String> classPathRoots, final List<String> specificationPath,
            Optional<Integer> evidenceLimit) {
        final ClasspathRoot classpathRoot = getAnalysisScope(classPathRoots);
        final List<Pair<String, Definition>> definitionsAndPaths = specificationPath.stream()
                .map(p -> Pair.make(p, compileSpecification(p))).collect(Collectors.toList());
        final ClassParser classParser = new ClassPathParser(includeAll);
        final ModuleAnalyser analyser = new ModuleAnalyser(classParser, classpathRoot, evidenceLimit, factory);
        final List<Definition> definitions = definitionsAndPaths.stream().map(p -> p.second)
                .collect(Collectors.toList());
        final List<AnalyserModel.AnalysisResult> results = analyser.analyse(definitions);
        boolean errors = false;
        for (int i = 0; i < definitionsAndPaths.size(); ++i) {
            final String path = definitionsAndPaths.get(i).first;
            final AnalysisMode mode = definitionsAndPaths.get(i).second.mode;
            final AnalyserModel.AnalysisResult result = results.get(i);
            if (mode == AnalysisMode.STRICT) {
                errors = errors | strictAnalysis(path, result);
            } else {
                errors = errors | looseAnalysis(path, result);
            }
        }
        if (errors) {
            throw new AnalyserException("Analysis failed");
        }
    }

    private Definition compileSpecification(final String specificationPath) {
        final File specificationFile = new File(specificationPath);
        if (!specificationFile.exists() || specificationFile.isDirectory() || !specificationFile.canRead()) {
            throw new AnalyserException(String.format("Cannot read from specification file '%s'.", specificationPath));
        }
        printer.info(String.format("Compiling specification '%s'", specificationPath));
        final SyntaxTree.Definition syntaxDefinition = getDefinition(specificationFile);
        final Definition definition = compileDefinition(syntaxDefinition);
        printer.info("Done!");
        return definition;
    }

    private ClasspathRoot getAnalysisScope(List<String> paths) {
        final List<File> jars = new ArrayList<>();
        final List<File> dirs = new ArrayList<>();
        final List<String> ignored = new ArrayList<>();
        for (String path : paths) {
            final File f = new File(path);
            if (!f.exists() || !f.canRead() || (f.isFile() && !path.endsWith(".jar"))) {
                ignored.add(path);
            } else if (f.isDirectory()) {
                dirs.add(f);
            } else {
                jars.add(f);
            }
        }
        pathEventSink.ignoredPaths(ignored);
        pathEventSink.directories(getPaths(dirs));
        pathEventSink.jars(getPaths(jars));

        final List<ClasspathRoot> classpathRoots = new ArrayList<>();
        for (File jar : jars) {
            classpathRoots.add(new ArchiveClassPathRoot(jar));
        }
        for (File dir : dirs) {
            classpathRoots.add(new DirectoryClassPathRoot(dir));
        }

        return new CompoundClassPathRoot(classpathRoots);
    }

    private List<String> getPaths(List<File> files) {
        final List<String> result = new ArrayList<>();
        for (File f : files) {
            result.add(f.getAbsolutePath());
        }
        return result;
    }

    private SyntaxTree.Definition getDefinition(File specificationFile) {
        final DefinitionParser definitionParser = new DefinitionParser();
        try {
            return definitionParser.parse(new FileReader(specificationFile));
        } catch (IOException e) {
            throw new AnalyserException("Error while parsing the specification file: " + e.getMessage());
        }
    }

    private static Definition compileDefinition(SyntaxTree.Definition definition) {
        final Compiler compiler = new Compiler();
        return compiler.compile(definition);
    }

    private boolean strictAnalysis(String path, AnalyserModel.AnalysisResult analysisResult) {
        printer.info(String.format("Starting strict analysis on '%s'", path));
        boolean error = !analysisResult.evidenceBackedViolations.isEmpty()
                || !analysisResult.moduleConnectionViolations.isEmpty();
        printMetrics(analysisResult.metrics);
        if (analysisResult.evidenceBackedViolations.isEmpty()) {
            strictAnalysisEventSink.dependenciesCorrect();
        } else {
            strictAnalysisEventSink.dependencyViolationsPresent();
            printDependencyViolations(analysisResult.evidenceBackedViolations);
        }
        if (analysisResult.moduleConnectionViolations.isEmpty()) {
            strictAnalysisEventSink.directDependenciesCorrect();
        } else {
            strictAnalysisEventSink.noDirectDependenciesViolationPresent();
            printNoDirectDependecyViolation(analysisResult.moduleConnectionViolations);
        }
        if (error) {
            printer.info(String.format("Analysis on '%s' failed", path));
        } else {
            printer.info(String.format("Analysis on '%s' complete", path));
        }
        return error;
    }

    private boolean looseAnalysis(String path, AnalyserModel.AnalysisResult analysisResult) {
        printer.info(String.format("Starting loose analysis on '%s'", path));
        printMetrics(analysisResult.metrics);
        boolean error = !analysisResult.moduleConnectionViolations.isEmpty()
                || !analysisResult.evidenceBackedViolations.isEmpty();
        if (analysisResult.moduleConnectionViolations.isEmpty()) {
            looseAnalysisEventSink.allDependenciesPresent();
        } else {
            looseAnalysisEventSink.absentDependencyViolationsPresent();
            printAbsentDependencies(analysisResult.moduleConnectionViolations);
        }
        if (analysisResult.evidenceBackedViolations.isEmpty()) {
            looseAnalysisEventSink.noUndesiredDependencies();
        } else {
            looseAnalysisEventSink.undesiredDependencyViolationsPresent();
            printUndesiredDependencies(analysisResult.evidenceBackedViolations);
        }
        if (error) {
            printer.info(String.format("Analysis on '%s' failed", path));
        } else {
            printer.info(String.format("Analysis on '%s' complete", path));
        }
        return error;
    }

    private void printMetrics(Collection<AnalyserModel.Metrics> metrics) {
        for (AnalyserModel.Metrics m : metrics) {
            measureEventSink.fanInOutMeasure(m.module, m.fanIn, m.fanOut);
        }
    }

    private void printDependencyViolations(Collection<AnalyserModel.EvidenceBackedViolation> violations) {
        for (AnalyserModel.EvidenceBackedViolation violation : violations) {
            strictAnalysisEventSink.dependencyViolation(violation.sourceModule, violation.destinationModule,
                    appendStartIfNotEmpty(violation.specificationPath, violation.sourceModule),
                    appendStartIfNotEmpty(violation.actualPath, violation.sourceModule), violation.evidences);
        }
    }

    private <T> List<T> appendStartIfNotEmpty(List<T> collection, T element) {
        if (collection.isEmpty()) {
            return collection;
        } else {
            final List<T> result = new ArrayList<>(collection.size() + 1);
            result.add(element);
            result.addAll(collection);
            return result;
        }
    }

    private void printNoDirectDependecyViolation(Collection<AnalyserModel.ModuleConnectionViolation> violations) {
        for (AnalyserModel.ModuleConnectionViolation violation : violations) {
            strictAnalysisEventSink.noDirectDependencyViolation(violation.sourceModule, violation.destinationModule);
        }
    }

    private void printAbsentDependencies(Collection<AnalyserModel.ModuleConnectionViolation> violations) {
        for (AnalyserModel.ModuleConnectionViolation violation : violations) {
            looseAnalysisEventSink.absentDependencyViolation(violation.sourceModule, violation.destinationModule);
        }
    }

    private void printUndesiredDependencies(Collection<AnalyserModel.EvidenceBackedViolation> violations) {
        for (AnalyserModel.EvidenceBackedViolation violation : violations) {
            looseAnalysisEventSink.undesiredDependencyViolation(violation.sourceModule, violation.destinationModule,
                    appendStartIfNotEmpty(violation.actualPath, violation.sourceModule), violation.evidences);
        }
    }

}
