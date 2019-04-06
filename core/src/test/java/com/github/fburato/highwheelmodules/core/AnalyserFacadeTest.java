package com.github.fburato.highwheelmodules.core;

import com.github.fburato.highwheelmodules.core.analysis.AnalyserException;
import com.github.fburato.highwheelmodules.core.specification.CompilerException;
import com.github.fburato.highwheelmodules.utils.Pair;
import org.jparsec.error.ParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.util.*;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AnalyserFacadeTest {
    private final List<String> accumulator = new ArrayList<>();

    private AnalyserFacade.Printer printer = spy(new FacadeTestUtils.AccumulatorPrinter(accumulator));

    private AnalyserFacade.EventSink.PathEventSink pathEventSink = spy(new FacadeTestUtils.AccumulatorPathEventSink(accumulator));

    private AnalyserFacade.EventSink.MeasureEventSink measureEventSink = spy(new FacadeTestUtils.AccumulatorMeasureEventSink(accumulator));

    private AnalyserFacade.EventSink.StrictAnalysisEventSink strictAnalysisEventSink = spy(new FacadeTestUtils.AccumulatorStrictAnalysisEvenSink(accumulator));

    private AnalyserFacade.EventSink.LooseAnalysisEventSink looseAnalysisEventSink = spy(new FacadeTestUtils.AccumulatorLooseAnalysisEventSink(accumulator));

    private AnalyserFacade testee;

    private final String defaultSpec = join(File.separator, Arrays.asList("src", "test", "resources", "spec.hwm"));
    private final String alternativeStrictSpec = join(File.separator, Arrays.asList("src", "test", "resources", "alternate-strict-spec.hwm"));
    private final String jarPath = join(File.separator, Arrays.asList("src", "test", "resources", "highwheel-model.jar"));
    private final String wrongSpec = join(File.separator, Arrays.asList("src", "test", "resources", "wrong-syntax-spec.hwm"));
    private final String wrongSemanticsSpec =
            join(File.separator, Arrays.asList("src", "test", "resources", "wrong-semantics-spec.hwm"));
    private final String wrongStrictDefinitionSpec =
            join(File.separator, Arrays.asList("src", "test", "resources", "wrong-strict-spec.hwm"));
    private final String looseSpec = join(File.separator, Arrays.asList("src", "test", "resources", "loose-spec.hwm"));
    private final String orgExamplePath = join(File.separator, Arrays.asList("target", "test-classes", "org"));
    private final String wrongLooseDefinitionSpec =
            join(File.separator, Arrays.asList("src", "test", "resources", "wrong-loose-spec.hwm"));

    @BeforeEach
    public void setUp() {
        initMocks(this);
        testee = new AnalyserFacade(printer, pathEventSink, measureEventSink, strictAnalysisEventSink, looseAnalysisEventSink);
    }

    private static class CollectionContains implements ArgumentMatcher<List<String>> {

        private final String regex;

        public CollectionContains(String regex) {
            this.regex = regex;
        }

        @Override
        public boolean matches(List<String> strings) {
            return strings.stream().anyMatch((el) -> el.matches(regex));
        }
    }

    private static CollectionContains anyMatches(String regex) {
        return new CollectionContains(regex);
    }

    @Test
    public void shouldPrintAsInfoJarsThatArePassedAsArgument() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(jarPath), one(defaultSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                verify(pathEventSink).jars(argThat(anyMatches(".*highwheel-model\\.jar.*")));
            }
        });
    }

    @Test
    public void shouldPrintAsInfoDirectoriesThatPassedAsArgument() {
        testee.runAnalysis(Arrays.asList(orgExamplePath), one(defaultSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
        verify(pathEventSink).directories(argThat(anyMatches(".*test-classes.*org.*")));
    }

    @Test
    public void shouldPrintAsIgnoredFileThatDoNotExist() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList("foobar"), one(defaultSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                verify(pathEventSink).ignoredPaths(argThat(anyMatches(".*foobar.*")));
            }
        });
    }

    @Test
    public void shouldPrintAsInfoJarsDiresAndIgnored() {
        testee.runAnalysis(Arrays.asList(jarPath, orgExamplePath, "foobar"), one(defaultSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
        verify(pathEventSink).jars(argThat(anyMatches(".*highwheel-model\\.jar.*")));
        verify(pathEventSink).directories(argThat(anyMatches(".*test-classes.*org.*")));
        verify(pathEventSink).ignoredPaths(argThat(anyMatches(".*foobar.*")));
    }

    @Test
    public void shoulFailIfSpecificationFileDoesNotExist() {
        assertThrows(AnalyserException.class, () ->
                testee.runAnalysis(Arrays.asList(orgExamplePath), one("foobar"), AnalyserFacade.ExecutionMode.STRICT, Optional.empty())
        );
    }

    @Test
    public void shouldFailIfParsingFails() {
        assertThrows(ParserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(orgExamplePath), one(wrongSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                verify(printer).info(matches(".*Compiling specification.*"));
            }
        });
    }

    @Test
    public void shouldFailIfCompilationFails() {
        assertThrows(CompilerException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(orgExamplePath), one(wrongSemanticsSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                verify(printer).info(matches(".*Compiling specification.*"));
            }
        });
    }

    @Test
    public void strictAnalysisShouldProduceTheExpectedOutputWhenThereAreNoViolation() {
        testee.runAnalysis(Arrays.asList(orgExamplePath), one(defaultSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
        verify(strictAnalysisEventSink).dependenciesCorrect();
        verify(strictAnalysisEventSink).directDependenciesCorrect();
    }

    @Test
    public void looseAnalysisShouldProduceTheExpectedOutputWhenThereAreNoViolation() {
        testee.runAnalysis(Arrays.asList(orgExamplePath), one(looseSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.empty());
        verify(looseAnalysisEventSink).allDependenciesPresent();
        verify(looseAnalysisEventSink).noUndesiredDependencies();
    }

    @Test
    public void strictAnalysisShouldProduceMetrics() {
        testee.runAnalysis(Arrays.asList(orgExamplePath), one(defaultSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
        verifyStrictMetrics();
    }

    private void verifyStrictMetrics() {
        verify(measureEventSink).fanInOutMeasure("Facade", 2, 3);
        verify(measureEventSink).fanInOutMeasure("Utils", 2, 0);
        verify(measureEventSink).fanInOutMeasure("IO", 1, 3);
        verify(measureEventSink).fanInOutMeasure("Model", 4, 0);
        verify(measureEventSink).fanInOutMeasure("CoreInternals", 1, 3);
        verify(measureEventSink).fanInOutMeasure("CoreApi", 4, 1);
        verify(measureEventSink).fanInOutMeasure("Controller", 1, 1);
        verify(measureEventSink).fanInOutMeasure("Main", 0, 4);
    }

    @Test
    public void looseAnalysisShouldProduceMetrics() {
        testee.runAnalysis(Arrays.asList(orgExamplePath), one(looseSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.empty());
        verifyLooseMetrics();
    }

    private void verifyLooseMetrics() {
        verify(measureEventSink).fanInOutMeasure("Facade", 2, 3);
        verify(measureEventSink).fanInOutMeasure("Utils", 2, 0);
        verify(measureEventSink).fanInOutMeasure("IO", 1, 3);
        verify(measureEventSink).fanInOutMeasure("Model", 4, 0);
        verify(measureEventSink).fanInOutMeasure("CoreInternals", 1, 2);
        verify(measureEventSink).fanInOutMeasure("CoreApi", 3, 1);
        verify(measureEventSink).fanInOutMeasure("Controller", 1, 1);
        verify(measureEventSink).fanInOutMeasure("Main", 0, 4);
    }

    @Test
    public void strictAnalysisShouldFailAndPrintTheViolations() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(orgExamplePath), one(wrongStrictDefinitionSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                verify(strictAnalysisEventSink).dependencyViolationsPresent();
                verify(strictAnalysisEventSink).dependencyViolation("IO", "Utils", Collections.emptyList(),
                        Arrays.asList("IO", "Utils"),
                        Arrays.asList(Arrays.asList(
                                Pair.make("org.example.io.IOImplementaion:something", "org.example.commons.Utility:util"),
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util"),
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util1")
                        )));
                verify(strictAnalysisEventSink).noDirectDependenciesViolationPresent();
                verify(strictAnalysisEventSink).noDirectDependencyViolation("Facade", "CoreInternals");
            }
        });
    }

    @Test
    public void strictAnalysisShouldFailAndPrintLimitedViolations() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(orgExamplePath), one(wrongStrictDefinitionSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.of(1));
            } finally {
                verify(strictAnalysisEventSink).dependencyViolationsPresent();
                verify(strictAnalysisEventSink).dependencyViolation("IO", "Utils", Collections.emptyList(),
                        Arrays.asList("IO", "Utils"),
                        Arrays.asList(Arrays.asList(
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util")
                        )));
                verify(strictAnalysisEventSink).noDirectDependenciesViolationPresent();
                verify(strictAnalysisEventSink).noDirectDependencyViolation("Facade", "CoreInternals");
            }
        });
    }

    @Test
    public void looseAnalysisShouldFailAndPrintTheViolations() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(orgExamplePath), one(wrongLooseDefinitionSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.empty());
            } finally {
                verify(looseAnalysisEventSink).absentDependencyViolationsPresent();
                verify(looseAnalysisEventSink).undesiredDependencyViolationsPresent();
                verify(looseAnalysisEventSink).absentDependencyViolation("IO", "CoreInternals");
                verify(looseAnalysisEventSink).undesiredDependencyViolation("IO", "Model",
                        Arrays.asList("IO", "Model"),
                        Arrays.asList(Arrays.asList(
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1:(init)"),
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1")
                        )));
            }
        });
    }

    @Test
    public void looseAnalysisShouldFailAndPrintLimitedViolations() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Arrays.asList(orgExamplePath), one(wrongLooseDefinitionSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.of(1));
            } finally {
                verify(looseAnalysisEventSink).absentDependencyViolationsPresent();
                verify(looseAnalysisEventSink).undesiredDependencyViolationsPresent();
                verify(looseAnalysisEventSink).absentDependencyViolation("IO", "CoreInternals");
                verify(looseAnalysisEventSink).undesiredDependencyViolation("IO", "Model",
                        Arrays.asList("IO", "Model"),
                        Arrays.asList(Arrays.asList(
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1")
                        )));
            }
        });
    }

    private static <T> List<T> one(T t) {
        return Collections.singletonList(t);
    }

    @Test
    public void strictAnalysisShouldFailIfAnyOfTheSpecsDoesNotCompile() {
        assertThrows(ParserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(defaultSpec, wrongSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                assertThat(accumulator).containsExactly(
                        "IGNORED_PATHS - ",
                        "DIRECTORIES - 1",
                        "JARS - ",
                        "INFO - Compiling specification '" + defaultSpec + "'",
                        "INFO - Done!",
                        "INFO - Compiling specification '" + wrongSpec + "'"
                );
            }
        });
    }

    @Test
    public void strictAnalysisShouldCompleteAllSpecificationSuccessfully() {
        testee.runAnalysis(one(orgExamplePath), Arrays.asList(defaultSpec, alternativeStrictSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
        assertThat(accumulator).containsExactly(
                "IGNORED_PATHS - ",
                "DIRECTORIES - 1",
                "JARS - ",
                "INFO - Compiling specification '" + defaultSpec + "'",
                "INFO - Done!",
                "INFO - Compiling specification '" + alternativeStrictSpec + "'",
                "INFO - Done!",
                "INFO - Starting strict analysis on '" + defaultSpec + "'",
                "FAN_IN_OUT_MEASURE - Facade,2,3",
                "FAN_IN_OUT_MEASURE - Utils,2,0",
                "FAN_IN_OUT_MEASURE - IO,1,3",
                "FAN_IN_OUT_MEASURE - Model,4,0",
                "FAN_IN_OUT_MEASURE - CoreInternals,1,3",
                "FAN_IN_OUT_MEASURE - CoreApi,4,1",
                "FAN_IN_OUT_MEASURE - Controller,1,1",
                "FAN_IN_OUT_MEASURE - Main,0,4",
                "DEPENDENCIES_CORRECT",
                "DIRECT_DEPENDENCIES_CORRECT",
                "INFO - Analysis on '" + defaultSpec + "' complete",
                "INFO - Starting strict analysis on '" + alternativeStrictSpec + "'",
                "FAN_IN_OUT_MEASURE - Internals,1,0",
                "FAN_IN_OUT_MEASURE - Main,0,1",
                "DEPENDENCIES_CORRECT",
                "DIRECT_DEPENDENCIES_CORRECT",
                "INFO - Analysis on '" + alternativeStrictSpec + "' complete"
        );
    }

    @Test
    public void strictAnalysisShouldFailButCompleteAnalysisIfAnyOfTheAnalysisFail() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(wrongStrictDefinitionSpec, alternativeStrictSpec), AnalyserFacade.ExecutionMode.STRICT, Optional.empty());
            } finally {
                assertThat(accumulator).containsExactly(
                        "IGNORED_PATHS - ",
                        "DIRECTORIES - 1",
                        "JARS - ",
                        "INFO - Compiling specification '" + wrongStrictDefinitionSpec + "'",
                        "INFO - Done!",
                        "INFO - Compiling specification '" + alternativeStrictSpec + "'",
                        "INFO - Done!",
                        "INFO - Starting strict analysis on '" + wrongStrictDefinitionSpec + "'",
                        "FAN_IN_OUT_MEASURE - Facade,2,3",
                        "FAN_IN_OUT_MEASURE - Utils,2,0",
                        "FAN_IN_OUT_MEASURE - IO,1,3",
                        "FAN_IN_OUT_MEASURE - Model,4,0",
                        "FAN_IN_OUT_MEASURE - CoreInternals,1,3",
                        "FAN_IN_OUT_MEASURE - CoreApi,4,1",
                        "FAN_IN_OUT_MEASURE - Controller,1,1",
                        "FAN_IN_OUT_MEASURE - Main,0,4",
                        "DEPENDENCY_VIOLATION_PRESENT",
                        "DEPENDENCY_VIOLATION - {IO,Utils,[],[IO,Utils],[1]}",
                        "DEPENDENCY_VIOLATION - {Main,Utils,[Main,Facade,CoreInternals,Utils],[Main,IO,Utils],[2]}",
                        "NO_DIRECT_DEPENDENCIES_VIOLATION_PRESENT",
                        "NO_DIRECT_DEPENDENCY_VIOLATION - Facade,CoreInternals",
                        "INFO - Analysis on '" + wrongStrictDefinitionSpec + "' failed",
                        "INFO - Starting strict analysis on '" + alternativeStrictSpec + "'",
                        "FAN_IN_OUT_MEASURE - Internals,1,0",
                        "FAN_IN_OUT_MEASURE - Main,0,1",
                        "DEPENDENCIES_CORRECT",
                        "DIRECT_DEPENDENCIES_CORRECT",
                        "INFO - Analysis on '" + alternativeStrictSpec + "' complete"
                );
            }
        });
    }

    @Test
    public void looseAnalysisShouldFailIfAnyOfTheSpecsDoesNotCompile() {
        assertThrows(ParserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(looseSpec, wrongSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.of(0));
            } finally {
                assertThat(accumulator).containsExactly(
                        "IGNORED_PATHS - ",
                        "DIRECTORIES - 1",
                        "JARS - ",
                        "INFO - Compiling specification '" + looseSpec + "'",
                        "INFO - Done!",
                        "INFO - Compiling specification '" + wrongSpec + "'"
                );
            }
        });
    }

    @Test
    public void looseAnalysisShouldCompleteAllSpecificationSuccessfully() {
        testee.runAnalysis(one(orgExamplePath), Arrays.asList(looseSpec, alternativeStrictSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.of(0));
        assertThat(accumulator).containsExactly(
                "IGNORED_PATHS - ",
                "DIRECTORIES - 1",
                "JARS - ",
                "INFO - Compiling specification '" + looseSpec + "'",
                "INFO - Done!",
                "INFO - Compiling specification '" + alternativeStrictSpec + "'",
                "INFO - Done!",
                "INFO - Starting loose analysis on '" + looseSpec + "'",
                "FAN_IN_OUT_MEASURE - Facade,2,3",
                "FAN_IN_OUT_MEASURE - Utils,2,0",
                "FAN_IN_OUT_MEASURE - IO,1,3",
                "FAN_IN_OUT_MEASURE - Model,4,0",
                "FAN_IN_OUT_MEASURE - CoreInternals,1,2",
                "FAN_IN_OUT_MEASURE - CoreApi,3,1",
                "FAN_IN_OUT_MEASURE - Controller,1,1",
                "FAN_IN_OUT_MEASURE - Main,0,4",
                "ALL_DEPENDENCIES_PRESENT",
                "NO_UNDESIRED_DEPENDENCIES",
                "INFO - Analysis on '" + looseSpec + "' complete",
                "INFO - Starting loose analysis on '" + alternativeStrictSpec + "'",
                "FAN_IN_OUT_MEASURE - Internals,1,0",
                "FAN_IN_OUT_MEASURE - Main,0,1",
                "ALL_DEPENDENCIES_PRESENT",
                "NO_UNDESIRED_DEPENDENCIES",
                "INFO - Analysis on '" + alternativeStrictSpec + "' complete"
        );
    }

    @Test
    public void looseAnalysisShouldFailButCompleteAnalysisIfAnyOfTheAnalysisFail() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(wrongLooseDefinitionSpec, looseSpec), AnalyserFacade.ExecutionMode.LOOSE, Optional.of(0));
            } finally {
                assertThat(accumulator).containsExactly(
                        "IGNORED_PATHS - ",
                        "DIRECTORIES - 1",
                        "JARS - ",
                        "INFO - Compiling specification '" + wrongLooseDefinitionSpec + "'",
                        "INFO - Done!",
                        "INFO - Compiling specification '" + looseSpec + "'",
                        "INFO - Done!",
                        "INFO - Starting loose analysis on '" + wrongLooseDefinitionSpec + "'",
                        "FAN_IN_OUT_MEASURE - Facade,2,3",
                        "FAN_IN_OUT_MEASURE - Utils,2,0",
                        "FAN_IN_OUT_MEASURE - IO,1,3",
                        "FAN_IN_OUT_MEASURE - Model,4,0",
                        "FAN_IN_OUT_MEASURE - CoreInternals,1,2",
                        "FAN_IN_OUT_MEASURE - CoreApi,3,1",
                        "FAN_IN_OUT_MEASURE - Controller,1,1",
                        "FAN_IN_OUT_MEASURE - Main,0,4",
                        "ABSENT_DEPENDENCY_VIOLATIONS_PRESENT",
                        "ABSENT_DEPENDENCY_VIOLATION - IO,CoreInternals",
                        "UNDESIRED_DEPENDENCY_VIOLATION_PRESENT",
                        "UNDESIRED_DEPENDENCY_VIOLATION - {IO,Model,[IO,Model],[1]}",
                        "INFO - Analysis on '" + wrongLooseDefinitionSpec + "' failed",
                        "INFO - Starting loose analysis on '" + looseSpec + "'",
                        "FAN_IN_OUT_MEASURE - Facade,2,3",
                        "FAN_IN_OUT_MEASURE - Utils,2,0",
                        "FAN_IN_OUT_MEASURE - IO,1,3",
                        "FAN_IN_OUT_MEASURE - Model,4,0",
                        "FAN_IN_OUT_MEASURE - CoreInternals,1,2",
                        "FAN_IN_OUT_MEASURE - CoreApi,3,1",
                        "FAN_IN_OUT_MEASURE - Controller,1,1",
                        "FAN_IN_OUT_MEASURE - Main,0,4",
                        "ALL_DEPENDENCIES_PRESENT",
                        "NO_UNDESIRED_DEPENDENCIES",
                        "INFO - Analysis on '" + looseSpec + "' complete"
                );
            }
        });
    }
}
