package com.github.fburato.highwheelmodules.core;

import com.github.fburato.highwheelmodules.utils.Pair;

import java.util.List;
import java.util.Optional;

public interface AnalyserFacade {
    interface Printer {
        void info(String msg);
    }

    interface EventSink {
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

    void runAnalysis(final List<String> classPathRoots, final List<String> specificationPath,
            final Optional<Integer> evidenceLimit);
}
