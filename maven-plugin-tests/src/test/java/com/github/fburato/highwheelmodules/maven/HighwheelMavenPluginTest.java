package com.github.fburato.highwheelmodules.maven;

import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class HighwheelMavenPluginTest {

  private Verifier verifier;
  private final String analyseGoal = "com.github.fburato:highwheel-modules-maven-plugin:analyse";
  private final List<String> analysisGoals = Arrays.asList("compile", analyseGoal);

  @BeforeEach
  public void setUp() throws Exception {
    verifier = new Verifier(Paths
        .get("target", "test-classes", "test-example")
        .toAbsolutePath()
        .toString()
    );
  }

  private String runPluginAndReturnLog(boolean expectFailure) {
    return runPluginAndReturnLog(verifier,expectFailure);
  }

  private String runPluginAndReturnLog(Verifier verifier, boolean expectFailure) {
    try {
      verifier.executeGoals(analysisGoals);
      if (expectFailure) {
        fail("Failure expected");
      }
      return contentOf(Paths.get(verifier.getBasedir(), verifier.getLogFileName()).toFile());
    } catch (Exception e) {
      return contentOf(Paths.get(verifier.getBasedir(), verifier.getLogFileName()).toFile());
    }
  }

  @Test
  public void shouldSucceedOnStrictBaseSpecification() {
    final String logFile = runPluginAndReturnLog(false);
    assertThat(logFile)
        .matches("(?s).*Facade --> fanIn:     2, fanOut:     3.*")
        .matches("(?s).*Utils --> fanIn:     2, fanOut:     0.*")
        .matches("(?s).*IO --> fanIn:     1, fanOut:     3.*")
        .matches("(?s).*Model --> fanIn:     4, fanOut:     0.*")
        .matches("(?s).*CoreInternals --> fanIn:     1, fanOut:     3.*")
        .matches("(?s).*CoreApi --> fanIn:     4, fanOut:     1.*")
        .matches("(?s).*Controller --> fanIn:     1, fanOut:     1.*")
        .matches("(?s).*Main --> fanIn:     0, fanOut:     4.*")
        .matches("(?s).*No dependency violation detected.*")
        .matches("(?s).*No direct dependency violation detected.*");
  }

  @Test
  public void shouldFailOnNonExistingSpec() {
    verifier.getCliOptions().add("-DhwmSpecFiles=not-a-spec");
    final String logFile = runPluginAndReturnLog(true);
    assertThat(logFile)
        .matches("(?s).*Cannot read from specification file 'not-a-spec'.*");
  }

  @Test
  public void shouldFailOnWrongSpec() {
    verifier.getCliOptions().add("-DhwmSpecFiles=wrong-strict-spec.hwm");
    final String logFile = runPluginAndReturnLog(true);
    assertThat(logFile)
        .matches("(?s).*The following dependencies violate the specification:.*")
        .matches("(?s).*The following direct dependencies violate the specification:.*");
  }

  @Test
  public void shouldRunMultipleAnalysisInOnePassSuccessfully() {
    verifier.getCliOptions().add("-DhwmSpecFiles=spec.hwm,spec-with-prefix.hwm");
    final String logFile = runPluginAndReturnLog(false);
    assertThat(logFile)
        .matches("(?s).*Starting strict analysis on.*spec.hwm'.*" +
            "No dependency violation detected.*" +
            "No direct dependency violation detected.*" +
            "Analysis on .*spec.hwm.* complete.*" +
            "Starting strict analysis on .*spec-with-prefix.hwm.*" +
            "No dependency violation detected.*" +
            "No direct dependency violation detected.*" +
            "Analysis on .*spec-with-prefix.hwm.* complete.*");
  }

  @Test
  public void shouldAcceptSpecificationWithPrefix() {
    verifier.getCliOptions().add("-DhwmSpecFiles=spec-with-prefix.hwm");
    final String logFile = runPluginAndReturnLog(false);
    assertThat(logFile)
        .matches("(?s).*No dependency violation detected.*")
        .matches("(?s).*No direct dependency violation detected.*");
  }

  @Test
  public void shouldRunLooseAnalysis() {
    verifier.getCliOptions().add("-DhwmSpecFiles=loose-spec.hwm -DhwmAnalysisMode=loose");
    final String logFile = runPluginAndReturnLog(false);
    assertThat(logFile)
        .matches("(?s).*All dependencies specified exist.*")
        .matches("(?s).*No dependency violation detected.*");
  }

  @Test
  public void shouldFailIfAnyOfMultipleAnalysisFails() {
    verifier.getCliOptions().add("-DhwmSpecFiles=spec.hwm,loose-spec.hwm");
    final String logFile = runPluginAndReturnLog(true);
    assertThat(logFile)
        .matches(
            "(?s).*Starting strict analysis on.*spec.hwm'.*" +
                "No dependency violation detected.*" +
                "No direct dependency violation detected.*" +
                "Analysis on .*spec.hwm.* complete.*" +
                "Starting strict analysis on .*loose-spec.hwm.*" +
                "The following dependencies violate the specification:.*" +
                "No direct dependency violation detected.*" +
                "Analysis on .*loose-spec.hwm.* failed.*"
        );
  }

  @Test
  public void shouldReportEvidenceInCaseOfFailure() {
    verifier.getCliOptions().add("-DhwmSpecFiles=loose-spec.hwm");
    final String logFile = runPluginAndReturnLog(true);
    assertThat(logFile)
        .matches("(?s).*Facade -> Utils. Expected path: \\(empty\\), Actual module path: Facade -> CoreInternals -> Utils.*" +
            "Actual evidence paths:.*" +
            "Facade -> CoreInternals:.*" +
            "org.example.core.CoreFacade:facadeMethod1 -> org.example.core.internals.BusinessLogic1.*");
  }

  @Test
  public void shouldNotReportEvidenceIfEvidenceLimitIs0() {
    verifier.getCliOptions().add("-DhwmSpecFiles=loose-spec.hwm  -DhwmEvidenceLimit=0");
    final String logFile = runPluginAndReturnLog(true);
    assertThat(logFile)
        .matches("(?s).*Facade -> Utils. Expected path: \\(empty\\), Actual module path: Facade -> CoreInternals -> Utils.*" +
            "Facade -> Model. Expected path: Facade -> CoreInternals -> Model, Actual module path: Facade -> Model.*")
        .doesNotContainPattern("(?s).*Actual evidence paths:.*");
  }

  private Verifier multiModuleVerifier() throws Exception{
    return new Verifier(Paths
        .get("target", "test-classes", "multi-module")
        .toAbsolutePath()
        .toString()
    );
  }

  @Test
  public void shouldRunAnalysisOnAllSubmodulesFromParent() throws Exception {
    final Verifier multiModule = multiModuleVerifier();
    multiModule.setAutoclean(false);
    multiModule.executeGoal("clean");
    multiModule.executeGoal("compile");
    final String logContent = runPluginAndReturnLog(multiModule,false);
    assertThat(logContent)
        .matches("(?s).*Directories:.*moduleA.target.classes.*moduleB.target.classes.*moduleC.target.classes.*" +
            "Starting strict analysis on.*test-classes.multi-module.spec\\.hwm.*" +
            "Starting strict analysis on.*test-classes.multi-module.moduleA.spec\\.hwm.*" +
            "Starting strict analysis on.*test-classes.multi-module.moduleB.spec\\.hwm.*" +
            "Starting strict analysis on.*test-classes.multi-module.moduleC.spec\\.hwm.*");
  }

  @Test
  public void shouldRunAnalysisOnlyOnChildrenIfFlagIsSet() throws Exception {
    final Verifier multiModule = multiModuleVerifier();
    multiModule.setAutoclean(false);
    multiModule.executeGoal("clean");
    multiModule.executeGoal("compile");
    multiModule.addCliOption("-DhwmChildOnly=true");
    final String logContent = runPluginAndReturnLog(multiModule,false);
    assertThat(logContent)
        .matches("(?s).*Starting strict analysis on.*test-classes.multi-module.moduleA.spec\\.hwm.*" +
            "Starting strict analysis on.*test-classes.multi-module.moduleB.spec\\.hwm.*" +
            "Starting strict analysis on.*test-classes.multi-module.moduleC.spec\\.hwm.*")
        .doesNotMatch("(?s).*Directories:.*moduleA.target.classes.*moduleB.target.classes.*moduleC.target.classes.*" +
    "Starting strict analysis on.*test-classes.multi-module.spec\\.hwm.*")
        .matches("(?s).*Skipping pom project.*");
  }

  @Test
  public void shouldRunAnalysisOnlyOnParenIfFlagIsSet() throws Exception {
    final Verifier multiModule = multiModuleVerifier();
    multiModule.setAutoclean(false);
    multiModule.executeGoal("clean");
    multiModule.executeGoal("compile");
    multiModule.addCliOption("-DhwmParentOnly=true");
    final String logContent = runPluginAndReturnLog(multiModule,false);
    assertThat(logContent)
        .doesNotMatch("(?s).*Starting strict analysis on.*test-classes.multi-module.moduleA.spec\\.hwm.*")
        .doesNotMatch("(?s).*Starting strict analysis on.*test-classes.multi-module.moduleB.spec\\.hwm.*")
        .doesNotMatch("(?s).*Starting strict analysis on.*test-classes.multi-module.moduleC.spec\\.hwm.*")
        .matches("(?s).*Directories:.*moduleA.target.classes.*moduleB.target.classes.*moduleC.target.classes.*" +
            "Starting strict analysis on.*test-classes.multi-module.spec\\.hwm.*")
        .matches("(?s).*Skipping non pom project.*" +
            "Skipping non pom project.*" +
            "Skipping non pom project.*");
  }
}
