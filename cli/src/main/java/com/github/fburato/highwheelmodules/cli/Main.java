package com.github.fburato.highwheelmodules.cli;

import com.github.fburato.highwheelmodules.core.*;
import com.github.fburato.highwheelmodules.utils.Pair;

import java.util.List;

public class Main {

    private static class SystemPrinter implements Printer {

        @Override
        public void info(String msg) {
            System.out.println(" - " + msg);
        }
    }

    private static class SystemPathEventSink implements PathEventSink {

        @Override
        public void ignoredPaths(List<String> ignored) {
            System.out.println(" - Ignored: " + String.join(", ", ignored));
        }

        @Override
        public void directories(List<String> directories) {
            System.out.println(" - Directories: " + String.join(", ", directories));
        }

        @Override
        public void jars(List<String> jars) {
            System.out.println(" - Jars: " + String.join(", ", jars));
        }
    }

    private static class SystemMeasureSink implements MeasureEventSink {

        @Override
        public void fanInOutMeasure(String module, int fanIn, int fanOut) {
            System.out.println(String.format("  - %20s --> fanIn: %5d, fanOut: %5d", module, fanIn, fanOut));
        }
    }

    private static class SystemStrictAnalysisSink implements StrictAnalysisEventSink {

        @Override
        public void dependenciesCorrect() {
            System.out.println(" - No dependency violation detected");
        }

        @Override
        public void directDependenciesCorrect() {
            System.out.println(" - No direct dependency violation detected");
        }

        @Override
        public void dependencyViolationsPresent() {
            System.err.println(" - The following dependencies violate the specification:");
        }

        @Override
        public void dependencyViolation(String sourceModule, String destModule, List<String> expectedPath,
                List<String> actualPath, List<List<Pair<String, String>>> evidencePath) {
            System.err.println(
                    String.format("  %s -> %s. Expected path: %s, Actual module path: %s\n    Actual usage paths:\n%s",
                            sourceModule, destModule, printGraphPath(expectedPath), printGraphPath(actualPath),
                            printEvidences(actualPath, evidencePath)));
        }

        @Override
        public void noDirectDependenciesViolationPresent() {
            System.err.println(" - The following direct dependencies violate the specification:");
        }

        @Override
        public void noDirectDependencyViolation(String sourceModule, String destModule) {
            System.err.println(String.format("  %s -> %s", sourceModule, destModule));
        }
    }

    private static class SystemLooseAnalysisEventSink implements LooseAnalysisEventSink {

        @Override
        public void allDependenciesPresent() {
            System.out.println(" - All dependencies specified exist");
        }

        @Override
        public void noUndesiredDependencies() {
            System.out.println(" - No dependency violation detected");
        }

        @Override
        public void absentDependencyViolationsPresent() {
            System.err.println(" - The following dependencies do not exist:");
        }

        @Override
        public void absentDependencyViolation(String sourceModule, String destModule) {
            System.err.println(String.format("  %s -> %s", sourceModule, destModule));
        }

        @Override
        public void undesiredDependencyViolationsPresent() {
            System.err.println(" - The following dependencies violate the specification:");
        }

        @Override
        public void undesiredDependencyViolation(String sourceModule, String destModule, List<String> path,
                List<List<Pair<String, String>>> evidences) {
            System.err.println(String.format("  %s -/-> %s. Actual module path: %s\n    Actual usage paths:\n%s",
                    sourceModule, destModule, printGraphPath(path), printEvidences(path, evidences)));
        }
    }

    private static String printGraphPath(List<String> pathComponents) {
        if (pathComponents.isEmpty()) {
            return "(empty)";
        } else {
            return String.join(" -> ", pathComponents);
        }
    }

    private static String printEvidences(List<String> modules, List<List<Pair<String, String>>> evidences) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < modules.size() - 1; ++i) {
            final String current = modules.get(i);
            final String next = modules.get(i + 1);
            final List<Pair<String, String>> currentToNextEvidences = evidences.get(i);
            result.append(String.format("      %s -> %s:\n", current, next));
            for (Pair<String, String> evidence : currentToNextEvidences) {
                result.append(String.format("        %s -> %s\n", evidence.first, evidence.second));
            }
        }
        return result.toString();
    }

    public static void main(String[] argv) {
        try {
            final CmdParser cmdParser = new CmdParser(argv);
            final Printer printer = new SystemPrinter();
            final AnalyserFacade facade = new AnalyserFacadeImpl(printer, new SystemPathEventSink(),
                    new SystemMeasureSink(), new SystemStrictAnalysisSink(), new SystemLooseAnalysisEventSink());
            facade.runAnalysis(cmdParser.argList, cmdParser.specificationFiles, cmdParser.evidenceLimit);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
