package com.github.fburato.highwheelmodules.maven;

import com.github.fburato.highwheelmodules.core.AnalyserFacade;
import com.github.fburato.highwheelmodules.utils.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.*;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;

@Mojo(name = "analyse")
public class ModuleAnalyserMojo extends AbstractMojo {

    private class MavenPrinter implements AnalyserFacade.Printer {

        @Override
        public void info(String msg) {
            getLog().info(msg);
        }
    }

    private class MavenPathEventSink implements AnalyserFacade.EventSink.PathEventSink {

        @Override
        public void ignoredPaths(List<String> ignored) {
            final String ignoredString = "Ignored: " + join(", ", ignored);
            if (ignored.isEmpty()) {
                getLog().info(ignoredString);
            } else {
                getLog().warn(ignoredString);
            }
        }

        @Override
        public void directories(List<String> directories) {
            getLog().info("Directories: " + join(", ", directories));
        }

        @Override
        public void jars(List<String> jars) {
            getLog().info("Jars: " + join(", ", jars));
        }
    }

    private class MavenMeasureSink implements AnalyserFacade.EventSink.MeasureEventSink {

        @Override
        public void fanInOutMeasure(String module, int fanIn, int fanOut) {
            getLog().info(String.format("  %20s --> fanIn: %5d, fanOut: %5d", module, fanIn, fanOut));
        }
    }

    private class MavenStrictAnalysisSink implements AnalyserFacade.EventSink.StrictAnalysisEventSink {

        @Override
        public void dependenciesCorrect() {
            getLog().info("No dependency violation detected");
        }

        @Override
        public void directDependenciesCorrect() {
            getLog().info("No direct dependency violation detected");
        }

        @Override
        public void dependencyViolationsPresent() {
            getLog().error("The following dependencies violate the specification:");
        }

        @Override
        public void dependencyViolation(String sourceModule, String destModule, List<String> expectedPath,
                List<String> actualPath, List<List<Pair<String, String>>> evidencePath) {
            getLog().error(String.format("  %s -> %s. Expected path: %s, Actual module path: %s", sourceModule,
                    destModule, printGraphPath(expectedPath), printGraphPath(actualPath)));

            if (evidenceLimit > 0) {
                getLog().error("    Actual evidence paths:");
                printEvidences(actualPath, evidencePath);
            }
        }

        @Override
        public void noDirectDependenciesViolationPresent() {
            getLog().error("The following direct dependencies violate the specification:");
        }

        @Override
        public void noDirectDependencyViolation(String sourceModule, String destModule) {
            getLog().error(String.format("  %s -> %s", sourceModule, destModule));
        }
    }

    private class MavenLooseAnalysisEventSink implements AnalyserFacade.EventSink.LooseAnalysisEventSink {

        @Override
        public void allDependenciesPresent() {
            getLog().info("All dependencies specified exist");
        }

        @Override
        public void noUndesiredDependencies() {
            getLog().info("No dependency violation detected");
        }

        @Override
        public void absentDependencyViolationsPresent() {
            getLog().error("The following dependencies do not exist:");
        }

        @Override
        public void absentDependencyViolation(String sourceModule, String destModule) {
            getLog().error(String.format("  %s -> %s", sourceModule, destModule));
        }

        @Override
        public void undesiredDependencyViolationsPresent() {
            getLog().error("The following dependencies violate the specification:");
        }

        @Override
        public void undesiredDependencyViolation(String sourceModule, String destModule, List<String> path,
                List<List<Pair<String, String>>> evidencePath) {
            getLog().error(String.format("  %s -/-> %s. Actual module path: %s", sourceModule, destModule,
                    printGraphPath(path)));
            if (evidenceLimit > 0) {
                getLog().error("    Actual evidence paths:");
                printEvidences(path, evidencePath);
            }
        }
    }

    private static String printGraphPath(List<String> pathComponents) {
        if (pathComponents.isEmpty()) {
            return "(empty)";
        } else {
            return join(" -> ", pathComponents);
        }
    }

    private void printEvidences(List<String> modules, List<List<Pair<String, String>>> evidences) {
        for (int i = 0; i < modules.size() - 1; ++i) {
            final String current = modules.get(i);
            final String next = modules.get(i + 1);
            final List<Pair<String, String>> currentToNextEvidences = evidences.get(i);
            getLog().error(String.format("      %s -> %s:", current, next));
            final int subListLimit = evidenceLimit < currentToNextEvidences.size() ? evidenceLimit
                    : currentToNextEvidences.size();
            for (Pair<String, String> evidence : currentToNextEvidences.subList(0, subListLimit)) {
                getLog().error(String.format("        %s -> %s", evidence.first, evidence.second));
            }
            if (subListLimit < currentToNextEvidences.size()) {
                getLog().error(String.format("        (%d connections skipped)",
                        (currentToNextEvidences.size() - subListLimit)));
            }
        }
    }

    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "hwmParentOnly", defaultValue = "false")
    private boolean parentOnly;

    @Parameter(property = "hwmChildOnly", defaultValue = "false")
    private boolean childOnly;

    @Parameter(property = "hwmSpecFiles", defaultValue = "spec.hwm")
    private String specFile;

    @Parameter(property = "hwmEvidenceLimit", defaultValue = "5")
    private int evidenceLimit;

    @Parameter(property = "hwmSkip", defaultValue = "false")
    private boolean hwmSkip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String packaging = project.getModel().getPackaging();

        if (packaging.equalsIgnoreCase("pom") && childOnly) {
            this.getLog().info(String.format("Skipping pom module %s for hwmChildOnly=true", project.getName()));
            return;
        }

        if (!packaging.equalsIgnoreCase("pom") && parentOnly) {
            this.getLog().info(String.format("Skipping non pom module %s for hwmParentOnly=true", project.getName()));
            return;
        }

        if (hwmSkip) {
            this.getLog().info(String.format("Skipping module %s for hwmSkip=true", project.getName()));
            return;
        }
        final File attemptSpecFileInBuild = new File(
                project.getBasedir().getAbsolutePath() + File.separator + specFile);
        if (attemptSpecFileInBuild.exists() && attemptSpecFileInBuild.canRead()) {
            specFile = attemptSpecFileInBuild.getAbsolutePath();
            getLog().info("Using specification file: " + specFile);
        }
        if (evidenceLimit < 0) {
            evidenceLimit = Integer.MAX_VALUE;
        }
        final List<String> roots = getRootsForProject(packaging);
        analyse(roots, specFile);
    }

    private List<String> getRootsForProject(final String packaging) {
        final List<String> childrenRoots = collectRootsForChildProjects();
        final List<String> roots = new ArrayList<>(childrenRoots);
        if (!packaging.equalsIgnoreCase("pom")) {
            roots.add(makeRootForProject(this.project));
        }
        return roots;
    }

    private List<String> collectRootsForChildProjects() {
        final List<String> roots = new ArrayList<>();
        for (final Object each : this.project.getCollectedProjects()) {
            final MavenProject project = (MavenProject) each;
            this.getLog().info("Including child project " + project.getName());
            roots.add(makeRootForProject(project));
        }
        return roots;
    }

    private String makeRootForProject(final MavenProject project) {
        return project.getBuild().getOutputDirectory();
    }

    private void analyse(List<String> roots, String specFilePath) throws MojoFailureException {
        try {
            final AnalyserFacade.Printer printer = new MavenPrinter();
            final AnalyserFacade facade = new AnalyserFacade(printer, new MavenPathEventSink(), new MavenMeasureSink(),
                    new MavenStrictAnalysisSink(), new MavenLooseAnalysisEventSink());
            facade.runAnalysis(roots, Arrays.asList(specFilePath.split(",")), Optional.of(evidenceLimit));
        } catch (Exception e) {
            throw new MojoFailureException("Error during analysis: " + e.getMessage());
        }
    }
}
