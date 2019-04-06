package com.github.fburato.highwheelmodules.core;

import com.github.fburato.highwheelmodules.utils.Pair;

import java.util.List;

public interface FacadeTestUtils {
    class AccumulatorPrinter implements AnalyserFacade.Printer {

        private final List<String> accumulator;

        public AccumulatorPrinter(List<String> accumulator) {
            this.accumulator = accumulator;
        }

        @Override
        public void info(String msg) {
            accumulator.add("INFO - " + msg);
        }
    }

    class AccumulatorPathEventSink implements AnalyserFacade.EventSink.PathEventSink {

        private final List<String> accumulator;

        public AccumulatorPathEventSink(List<String> accumulator) {
            this.accumulator = accumulator;
        }

        @Override
        public void ignoredPaths(List<String> ignored) {
            accumulator.add("IGNORED_PATHS - " + String.join(",", ignored));
        }

        @Override
        public void directories(List<String> directories) {
            accumulator.add("DIRECTORIES - " + directories.size());
        }

        @Override
        public void jars(List<String> jars) {
            accumulator.add("JARS - " + String.join(",", jars));
        }
    }

    class AccumulatorMeasureEventSink implements AnalyserFacade.EventSink.MeasureEventSink {

        private final List<String> accumulator;

        public AccumulatorMeasureEventSink(List<String> accumulator) {
            this.accumulator = accumulator;
        }

        @Override
        public void fanInOutMeasure(String module, int fanIn, int fanOut) {
            accumulator.add(String.format("FAN_IN_OUT_MEASURE - %s,%d,%d", module, fanIn, fanOut));
        }
    }

    class AccumulatorStrictAnalysisEvenSink implements AnalyserFacade.EventSink.StrictAnalysisEventSink {

        private final List<String> accumulator;

        public AccumulatorStrictAnalysisEvenSink(List<String> accumulator) {
            this.accumulator = accumulator;
        }

        @Override
        public void dependenciesCorrect() {
            accumulator.add("DEPENDENCIES_CORRECT");
        }

        @Override
        public void directDependenciesCorrect() {
            accumulator.add("DIRECT_DEPENDENCIES_CORRECT");
        }

        @Override
        public void dependencyViolationsPresent() {
            accumulator.add("DEPENDENCY_VIOLATION_PRESENT");
        }

        @Override
        public void dependencyViolation(String sourceModule, String destModule, List<String> expectedPath,
                List<String> actualPath, List<List<Pair<String, String>>> evidences) {
            accumulator.add(String.format("DEPENDENCY_VIOLATION - {%s,%s,[%s],[%s],[%d]}", sourceModule, destModule,
                    String.join(",", expectedPath), String.join(",", actualPath), evidences.size()));
        }

        @Override
        public void noDirectDependenciesViolationPresent() {
            accumulator.add("NO_DIRECT_DEPENDENCIES_VIOLATION_PRESENT");
        }

        @Override
        public void noDirectDependencyViolation(String sourceModule, String destModule) {
            accumulator.add(String.format("NO_DIRECT_DEPENDENCY_VIOLATION - %s,%s", sourceModule, destModule));
        }
    }

    class AccumulatorLooseAnalysisEventSink implements AnalyserFacade.EventSink.LooseAnalysisEventSink {
        private final List<String> accumulator;

        public AccumulatorLooseAnalysisEventSink(List<String> accumulator) {
            this.accumulator = accumulator;
        }

        @Override
        public void allDependenciesPresent() {
            accumulator.add("ALL_DEPENDENCIES_PRESENT");
        }

        @Override
        public void noUndesiredDependencies() {
            accumulator.add("NO_UNDESIRED_DEPENDENCIES");
        }

        @Override
        public void absentDependencyViolationsPresent() {
            accumulator.add("ABSENT_DEPENDENCY_VIOLATIONS_PRESENT");
        }

        @Override
        public void absentDependencyViolation(String sourceModule, String destModule) {
            accumulator.add(String.format("ABSENT_DEPENDENCY_VIOLATION - %s,%s", sourceModule, destModule));
        }

        @Override
        public void undesiredDependencyViolationsPresent() {
            accumulator.add("UNDESIRED_DEPENDENCY_VIOLATION_PRESENT");
        }

        @Override
        public void undesiredDependencyViolation(String sourceModule, String destModule, List<String> path,
                List<List<Pair<String, String>>> evidences) {
            accumulator.add(String.format("UNDESIRED_DEPENDENCY_VIOLATION - {%s,%s,[%s],[%d]}", sourceModule,
                    destModule, String.join(",", path), evidences.size()));
        }
    }
}
