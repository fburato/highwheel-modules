package com.github.fburato.highwheelmodules.core;

import com.github.fburato.highwheelmodules.core.analysis.AnalyserException;
import com.github.fburato.highwheelmodules.core.specification.CompilerException;
import com.github.fburato.highwheelmodules.utils.Pair;
import org.jparsec.error.ParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.util.*;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("AnalyserFacade runAnalysis")
class AnalyserFacadeTest {
    private final List<String> accumulator = new ArrayList<>();

    private AnalyserFacade.Printer printer = spy(new FacadeTestUtils.AccumulatorPrinter(accumulator));

    private AnalyserFacade.EventSink.PathEventSink pathEventSink = spy(
            new FacadeTestUtils.AccumulatorPathEventSink(accumulator));

    private AnalyserFacade.EventSink.MeasureEventSink measureEventSink = spy(
            new FacadeTestUtils.AccumulatorMeasureEventSink(accumulator));

    private AnalyserFacade.EventSink.StrictAnalysisEventSink strictAnalysisEventSink = spy(
            new FacadeTestUtils.AccumulatorStrictAnalysisEvenSink(accumulator));

    private AnalyserFacade.EventSink.LooseAnalysisEventSink looseAnalysisEventSink = spy(
            new FacadeTestUtils.AccumulatorLooseAnalysisEventSink(accumulator));

    private AnalyserFacade testee;

    private final String defaultSpec = join(File.separator, Arrays.asList("src", "test", "resources", "spec.hwm"));
    private final String defaultSpecWhiteBlack = join(File.separator,
            Arrays.asList("src", "test", "resources", "spec-whiteblack.hwm"));
    private final String wrongSpecWhiteBlack = join(File.separator,
            Arrays.asList("src", "test", "resources", "wrong-spec-whiteblack.hwm"));
    private final String alternativeStrictSpec = join(File.separator,
            Arrays.asList("src", "test", "resources", "alternate-strict-spec.hwm"));
    private final String jarPath = join(File.separator,
            Arrays.asList("src", "test", "resources", "highwheel-model.jar"));
    private final String wrongSpec = join(File.separator,
            Arrays.asList("src", "test", "resources", "wrong-syntax-spec.hwm"));
    private final String wrongSemanticsSpec = join(File.separator,
            Arrays.asList("src", "test", "resources", "wrong-semantics-spec.hwm"));
    private final String wrongStrictDefinitionSpec = join(File.separator,
            Arrays.asList("src", "test", "resources", "wrong-strict-spec.hwm"));
    private final String looseSpec = join(File.separator, Arrays.asList("src", "test", "resources", "loose-spec.hwm"));
    private final String looseSpecWhiteBlack = join(File.separator,
            Arrays.asList("src", "test", "resources", "loose-spec-whiteblack.hwm"));
    private final String wronglooseSpecWhiteBlack = join(File.separator,
            Arrays.asList("src", "test", "resources", "wrong-loose-spec-whiteblack.hwm"));
    private final String orgExamplePath = join(File.separator, Arrays.asList("target", "test-classes", "org"));
    private final String wrongLooseDefinitionSpec = join(File.separator,
            Arrays.asList("src", "test", "resources", "wrong-loose-spec.hwm"));

    @BeforeEach
    void setUp() {
        testee = new AnalyserFacade(printer, pathEventSink, measureEventSink, strictAnalysisEventSink,
                looseAnalysisEventSink);
    }

    private static class CollectionContains implements ArgumentMatcher<List<String>> {

        private final String regex;

        CollectionContains(String regex) {
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
    @DisplayName("should print as info jars that are passed as argument")
    void testPrintJar() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(jarPath), one(defaultSpec), Optional.empty());
            } finally {
                verify(pathEventSink).jars(argThat(anyMatches(".*highwheel-model\\.jar.*")));
            }
        });
    }

    @Test
    @DisplayName("should print as info directories passed as argument")
    void testPrintDirectories() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(defaultSpec), Optional.empty());
        verify(pathEventSink).directories(argThat(anyMatches(".*test-classes.*org.*")));
    }

    @Test
    @DisplayName("should print ignored files that do not exist as info")
    void testPrintIgnoredFiles() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList("foobar"), one(defaultSpec), Optional.empty());
            } finally {
                verify(pathEventSink).ignoredPaths(argThat(anyMatches(".*foobar.*")));
            }
        });
    }

    @Test
    @DisplayName("should print ignored directories and jars as info")
    void testPrintIgnoredJarAndDirs() {
        testee.runAnalysis(Arrays.asList(jarPath, orgExamplePath, "foobar"), one(defaultSpec), Optional.empty());
        verify(pathEventSink).jars(argThat(anyMatches(".*highwheel-model\\.jar.*")));
        verify(pathEventSink).directories(argThat(anyMatches(".*test-classes.*org.*")));
        verify(pathEventSink).ignoredPaths(argThat(anyMatches(".*foobar.*")));
    }

    @Test
    @DisplayName("should fail if specification does not exist")
    void testFailOnNotExistingSpec() {
        assertThrows(AnalyserException.class,
                () -> testee.runAnalysis(Collections.singletonList(orgExamplePath), one("foobar"), Optional.empty()));
    }

    @Test
    @DisplayName("should fail on parse failure")
    void shouldFailIfParsingFails() {
        assertThrows(ParserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongSpec), Optional.empty());
            } finally {
                verify(printer).info(matches(".*Compiling specification.*"));
            }
        });
    }

    @Test
    @DisplayName("should fail on compilation failure")
    void shouldFailIfCompilationFails() {
        assertThrows(CompilerException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongSemanticsSpec),
                        Optional.empty());
            } finally {
                verify(printer).info(matches(".*Compiling specification.*"));
            }
        });
    }

    @Test
    @DisplayName("should produce strict analysis output on strict specification")
    void strictAnalysisShouldProduceTheExpectedOutputWhenThereAreNoViolation() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(defaultSpec), Optional.empty());
        verify(strictAnalysisEventSink).dependenciesCorrect();
        verify(strictAnalysisEventSink).directDependenciesCorrect();
    }

    @Test
    @DisplayName("should produce strict analysis output with white and blacklist specification")
    void testStrictWhiteBlack() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(defaultSpecWhiteBlack), Optional.empty());
        verify(strictAnalysisEventSink).dependenciesCorrect();
        verify(strictAnalysisEventSink).directDependenciesCorrect();
    }

    @Test
    @DisplayName("should fail on strict analysis with white and blacklist")
    void testWrongStrictWhiteBlack() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongSpecWhiteBlack),
                        Optional.empty());
            } finally {
                verify(strictAnalysisEventSink).dependencyViolationsPresent();
                verify(strictAnalysisEventSink).dependencyViolation("Main", "Facade",
                        Arrays.asList("Main", "Controller", "Facade"), Collections.emptyList(),
                        Collections.emptyList());
            }
        });
    }

    @Test
    @DisplayName("should produce loose output when no violation occurs")
    void looseAnalysisShouldProduceTheExpectedOutputWhenThereAreNoViolation() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(looseSpec), Optional.empty());
        verify(looseAnalysisEventSink).allDependenciesPresent();
        verify(looseAnalysisEventSink).noUndesiredDependencies();
    }

    @Test
    @DisplayName("should produce loose analysis output with white and blacklist")
    void testLooseWhiteBlack() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(looseSpecWhiteBlack), Optional.empty());
        verify(looseAnalysisEventSink).allDependenciesPresent();
        verify(looseAnalysisEventSink).noUndesiredDependencies();
    }

    @Test
    @DisplayName("should fail on loose analysis output with white and blacklist")
    void testWrongLooseWhiteBlack() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wronglooseSpecWhiteBlack),
                        Optional.empty());
            } finally {
                verify(looseAnalysisEventSink).absentDependencyViolationsPresent();
                verify(looseAnalysisEventSink).absentDependencyViolation("Main", "Controller");
            }
        });
    }

    @Test
    @DisplayName("should produce metrics on strict analysis")
    void testMetricsStrict() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(defaultSpec), Optional.empty());
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
    @DisplayName("should produce metrics on loose analysis")
    void looseAnalysisShouldProduceMetrics() {
        testee.runAnalysis(Collections.singletonList(orgExamplePath), one(looseSpec), Optional.empty());
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
    @DisplayName("should print violations on failed strict analysis")
    void testViolationsStrict() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongStrictDefinitionSpec),
                        Optional.empty());
            } finally {
                verify(strictAnalysisEventSink).dependencyViolationsPresent();
                verify(strictAnalysisEventSink).dependencyViolation("IO", "Utils", Collections.emptyList(),
                        Arrays.asList("IO", "Utils"),
                        Collections.singletonList(Arrays.asList(
                                Pair.make("org.example.io.IOImplementaion:something",
                                        "org.example.commons.Utility:util"),
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util"),
                                Pair.make("org.example.io.IOImplementaion:reader",
                                        "org.example.commons.Utility:util1"))));
                verify(strictAnalysisEventSink).noDirectDependenciesViolationPresent();
                verify(strictAnalysisEventSink).noDirectDependencyViolation("Facade", "CoreInternals");
            }
        });
    }

    @Test
    @DisplayName("should fail and limit the violations on strict analysis")
    void testLimitViolationStrict() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongStrictDefinitionSpec),
                        Optional.of(1));
            } finally {
                verify(strictAnalysisEventSink).dependencyViolationsPresent();
                verify(strictAnalysisEventSink).dependencyViolation("IO", "Utils", Collections.emptyList(),
                        Arrays.asList("IO", "Utils"), Collections.singletonList(Collections.singletonList(Pair
                                .make("org.example.io.IOImplementaion:reader", "org.example.commons.Utility:util"))));
                verify(strictAnalysisEventSink).noDirectDependenciesViolationPresent();
                verify(strictAnalysisEventSink).noDirectDependencyViolation("Facade", "CoreInternals");
            }
        });
    }

    @Test
    @DisplayName("should fail and print the violations on loose analysis")
    void testLimitViolationLoose() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongLooseDefinitionSpec),
                        Optional.empty());
            } finally {
                verify(looseAnalysisEventSink).absentDependencyViolationsPresent();
                verify(looseAnalysisEventSink).undesiredDependencyViolationsPresent();
                verify(looseAnalysisEventSink).absentDependencyViolation("IO", "CoreInternals");
                verify(looseAnalysisEventSink).undesiredDependencyViolation("IO", "Model", Arrays.asList("IO", "Model"),
                        Collections.singletonList(Arrays.asList(
                                Pair.make("org.example.io.IOImplementaion:reader",
                                        "org.example.core.model.Entity1:(init)"),
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1"))));
            }
        });
    }

    @Test
    @DisplayName("should fail and limit the violations on loose analysis")
    void testLooseLimitEvidence() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(Collections.singletonList(orgExamplePath), one(wrongLooseDefinitionSpec),
                        Optional.of(1));
            } finally {
                verify(looseAnalysisEventSink).absentDependencyViolationsPresent();
                verify(looseAnalysisEventSink).undesiredDependencyViolationsPresent();
                verify(looseAnalysisEventSink).absentDependencyViolation("IO", "CoreInternals");
                verify(looseAnalysisEventSink).undesiredDependencyViolation("IO", "Model", Arrays.asList("IO", "Model"),
                        Collections.singletonList(Collections.singletonList(
                                Pair.make("org.example.io.IOImplementaion:reader", "org.example.core.model.Entity1"))));
            }
        });
    }

    private static <T> List<T> one(T t) {
        return Collections.singletonList(t);
    }

    @Test
    @DisplayName("should fail if any of the specifications does not compile on strict")
    void testManyNotCompileStrict() {
        assertThrows(ParserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(defaultSpec, wrongSpec), Optional.empty());
            } finally {
                assertThat(accumulator).containsExactly("IGNORED_PATHS - ", "DIRECTORIES - 1", "JARS - ",
                        "INFO - Compiling specification '" + defaultSpec + "'", "INFO - Done!",
                        "INFO - Compiling specification '" + wrongSpec + "'");
            }
        });
    }

    @Test
    @DisplayName("should analyse all specifications successfully on strict analysis")
    void testAnalyseManyStrict() {
        testee.runAnalysis(one(orgExamplePath), Arrays.asList(defaultSpec, alternativeStrictSpec), Optional.empty());
        assertThat(accumulator).containsExactly("IGNORED_PATHS - ", "DIRECTORIES - 1", "JARS - ",
                "INFO - Compiling specification '" + defaultSpec + "'", "INFO - Done!",
                "INFO - Compiling specification '" + alternativeStrictSpec + "'", "INFO - Done!",
                "INFO - Starting strict analysis on '" + defaultSpec + "'", "FAN_IN_OUT_MEASURE - Facade,2,3",
                "FAN_IN_OUT_MEASURE - Utils,2,0", "FAN_IN_OUT_MEASURE - IO,1,3", "FAN_IN_OUT_MEASURE - Model,4,0",
                "FAN_IN_OUT_MEASURE - CoreInternals,1,3", "FAN_IN_OUT_MEASURE - CoreApi,4,1",
                "FAN_IN_OUT_MEASURE - Controller,1,1", "FAN_IN_OUT_MEASURE - Main,0,4", "DEPENDENCIES_CORRECT",
                "DIRECT_DEPENDENCIES_CORRECT", "INFO - Analysis on '" + defaultSpec + "' complete",
                "INFO - Starting strict analysis on '" + alternativeStrictSpec + "'",
                "FAN_IN_OUT_MEASURE - Internals,1,0", "FAN_IN_OUT_MEASURE - Main,0,1", "DEPENDENCIES_CORRECT",
                "DIRECT_DEPENDENCIES_CORRECT", "INFO - Analysis on '" + alternativeStrictSpec + "' complete");
    }

    @Test
    @DisplayName("should fail if one specification fails but complete successful analyses on strict")
    void testFailOneCompleteOthersStrict() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(wrongStrictDefinitionSpec, alternativeStrictSpec),
                        Optional.empty());
            } finally {
                assertThat(accumulator).containsExactly("IGNORED_PATHS - ", "DIRECTORIES - 1", "JARS - ",
                        "INFO - Compiling specification '" + wrongStrictDefinitionSpec + "'", "INFO - Done!",
                        "INFO - Compiling specification '" + alternativeStrictSpec + "'", "INFO - Done!",
                        "INFO - Starting strict analysis on '" + wrongStrictDefinitionSpec + "'",
                        "FAN_IN_OUT_MEASURE - Facade,2,3", "FAN_IN_OUT_MEASURE - Utils,2,0",
                        "FAN_IN_OUT_MEASURE - IO,1,3", "FAN_IN_OUT_MEASURE - Model,4,0",
                        "FAN_IN_OUT_MEASURE - CoreInternals,1,3", "FAN_IN_OUT_MEASURE - CoreApi,4,1",
                        "FAN_IN_OUT_MEASURE - Controller,1,1", "FAN_IN_OUT_MEASURE - Main,0,4",
                        "DEPENDENCY_VIOLATION_PRESENT", "DEPENDENCY_VIOLATION - {IO,Utils,[],[IO,Utils],[1]}",
                        "DEPENDENCY_VIOLATION - {Main,Utils,[Main,Facade,CoreInternals,Utils],[Main,IO,Utils],[2]}",
                        "NO_DIRECT_DEPENDENCIES_VIOLATION_PRESENT",
                        "NO_DIRECT_DEPENDENCY_VIOLATION - Facade,CoreInternals",
                        "INFO - Analysis on '" + wrongStrictDefinitionSpec + "' failed",
                        "INFO - Starting strict analysis on '" + alternativeStrictSpec + "'",
                        "FAN_IN_OUT_MEASURE - Internals,1,0", "FAN_IN_OUT_MEASURE - Main,0,1", "DEPENDENCIES_CORRECT",
                        "DIRECT_DEPENDENCIES_CORRECT", "INFO - Analysis on '" + alternativeStrictSpec + "' complete");
            }
        });
    }

    @Test
    @DisplayName("should fail if any of the specs does not compile in loose analysis")
    void testFailLooseAnalysis() {
        assertThrows(ParserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(looseSpec, wrongSpec), Optional.of(0));
            } finally {
                assertThat(accumulator).containsExactly("IGNORED_PATHS - ", "DIRECTORIES - 1", "JARS - ",
                        "INFO - Compiling specification '" + looseSpec + "'", "INFO - Done!",
                        "INFO - Compiling specification '" + wrongSpec + "'");
            }
        });
    }

    @Test
    @DisplayName("should fail if one specification fails but complete successful analyses on loose")
    void looseAnalysisShouldFailButCompleteAnalysisIfAnyOfTheAnalysisFail() {
        assertThrows(AnalyserException.class, () -> {
            try {
                testee.runAnalysis(one(orgExamplePath), Arrays.asList(wrongLooseDefinitionSpec, looseSpec),
                        Optional.of(0));
            } finally {
                assertThat(accumulator).containsExactly("IGNORED_PATHS - ", "DIRECTORIES - 1", "JARS - ",
                        "INFO - Compiling specification '" + wrongLooseDefinitionSpec + "'", "INFO - Done!",
                        "INFO - Compiling specification '" + looseSpec + "'", "INFO - Done!",
                        "INFO - Starting loose analysis on '" + wrongLooseDefinitionSpec + "'",
                        "FAN_IN_OUT_MEASURE - Facade,2,3", "FAN_IN_OUT_MEASURE - Utils,2,0",
                        "FAN_IN_OUT_MEASURE - IO,1,3", "FAN_IN_OUT_MEASURE - Model,4,0",
                        "FAN_IN_OUT_MEASURE - CoreInternals,1,2", "FAN_IN_OUT_MEASURE - CoreApi,3,1",
                        "FAN_IN_OUT_MEASURE - Controller,1,1", "FAN_IN_OUT_MEASURE - Main,0,4",
                        "ABSENT_DEPENDENCY_VIOLATIONS_PRESENT", "ABSENT_DEPENDENCY_VIOLATION - IO,CoreInternals",
                        "UNDESIRED_DEPENDENCY_VIOLATION_PRESENT",
                        "UNDESIRED_DEPENDENCY_VIOLATION - {IO,Model,[IO,Model],[1]}",
                        "INFO - Analysis on '" + wrongLooseDefinitionSpec + "' failed",
                        "INFO - Starting loose analysis on '" + looseSpec + "'", "FAN_IN_OUT_MEASURE - Facade,2,3",
                        "FAN_IN_OUT_MEASURE - Utils,2,0", "FAN_IN_OUT_MEASURE - IO,1,3",
                        "FAN_IN_OUT_MEASURE - Model,4,0", "FAN_IN_OUT_MEASURE - CoreInternals,1,2",
                        "FAN_IN_OUT_MEASURE - CoreApi,3,1", "FAN_IN_OUT_MEASURE - Controller,1,1",
                        "FAN_IN_OUT_MEASURE - Main,0,4", "ALL_DEPENDENCIES_PRESENT", "NO_UNDESIRED_DEPENDENCIES",
                        "INFO - Analysis on '" + looseSpec + "' complete");
            }
        });
    }

    @Test
    @DisplayName("should make alternate analyses successfully")
    void testLooseStrict() {
        testee.runAnalysis(one(orgExamplePath), Arrays.asList(looseSpec, alternativeStrictSpec), Optional.of(0));
        assertThat(accumulator).containsExactly("IGNORED_PATHS - ", "DIRECTORIES - 1", "JARS - ",
                "INFO - Compiling specification '" + looseSpec + "'", "INFO - Done!",
                "INFO - Compiling specification '" + alternativeStrictSpec + "'", "INFO - Done!",
                "INFO - Starting loose analysis on '" + looseSpec + "'", "FAN_IN_OUT_MEASURE - Facade,2,3",
                "FAN_IN_OUT_MEASURE - Utils,2,0", "FAN_IN_OUT_MEASURE - IO,1,3", "FAN_IN_OUT_MEASURE - Model,4,0",
                "FAN_IN_OUT_MEASURE - CoreInternals,1,2", "FAN_IN_OUT_MEASURE - CoreApi,3,1",
                "FAN_IN_OUT_MEASURE - Controller,1,1", "FAN_IN_OUT_MEASURE - Main,0,4", "ALL_DEPENDENCIES_PRESENT",
                "NO_UNDESIRED_DEPENDENCIES", "INFO - Analysis on '" + looseSpec + "' complete",
                "INFO - Starting strict analysis on '" + alternativeStrictSpec + "'",
                "FAN_IN_OUT_MEASURE - Internals,1,0", "FAN_IN_OUT_MEASURE - Main,0,1", "DEPENDENCIES_CORRECT",
                "DIRECT_DEPENDENCIES_CORRECT", "INFO - Analysis on '" + alternativeStrictSpec + "' complete");
    }
}
