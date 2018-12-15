package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.model.Definition;
import com.github.fburato.highwheelmodules.core.model.HWModule;
import com.github.fburato.highwheelmodules.core.model.rules.Dependency;
import com.github.fburato.highwheelmodules.core.model.rules.NoStrictDependency;
import com.github.fburato.highwheelmodules.utils.Pair;
import org.junit.Test;
import org.pitest.highwheel.bytecodeparser.ClassPathParser;
import org.pitest.highwheel.bytecodeparser.classpath.DirectoryClassPathRoot;
import org.pitest.highwheel.classpath.ClassParser;
import org.pitest.highwheel.classpath.ClasspathRoot;
import org.pitest.highwheel.cycles.Filter;
import org.pitest.highwheel.model.ElementName;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ModuleAnalyserTest {

  private final ClasspathRoot orgExamples =
      new DirectoryClassPathRoot(new File(join(File.separatorChar + "", Arrays.asList("target", "test-classes"))));
  private final Filter matchOnlyExampleDotOrg = new Filter() {
    @Override
    public boolean include(ElementName item) {
      return item.asJavaName().startsWith("org.example");
    }
  };

  private final ClassParser realClassParser = new ClassPathParser(matchOnlyExampleDotOrg);
  private final ClassParser classParser = spy(realClassParser);

  private final HWModule MAIN = HWModule.make("Main", "org.example.Main").get();
  private final HWModule CONTROLLER = HWModule.make("Controllers", "org.example.controller.*").get();
  private final HWModule FACADE = HWModule.make("Facade", "org.example.core.CoreFacade").get();
  private final HWModule COREINTERNALS = HWModule.make("CoreInternals", "org.example.core.internals.*").get();
  private final HWModule COREAPI = HWModule.make("CoreApi", "org.example.core.api.*").get();
  private final HWModule MODEL = HWModule.make("Model", "org.example.core.model.*").get();
  private final HWModule IO = HWModule.make("IO", "org.example.io.*").get();
  private final HWModule UTILS = HWModule.make("Commons", "org.example.commons.*").get();

  private ModuleAnalyser testee(ClasspathRoot root, Optional<Integer> evidenceLimit) {
    return new ModuleAnalyser(classParser, root, evidenceLimit);
  }

  @Test
  public void analyseStrictShouldAnalyseIfSpecificationIncludesOnlyOneModuleAndNoRules() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN),
        Collections.<Dependency>emptyList(),
        Collections.<NoStrictDependency>emptyList()
    );

    final AnalyserModel.StrictAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseStrict(one(definition)).get(0).second;

    assertThat(actual.dependencyViolations.isEmpty()).isTrue();
    assertThat(actual.noStrictDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.metrics).isEqualTo(Arrays.asList(met("Main", 0, 0)));
  }

  @Test(expected = AnalyserException.class)
  public void analyseStrictShouldFailIfNoModuleIsProvided() {
    final Definition definition = new Definition(Collections.<HWModule>emptyList(), Collections.<Dependency>emptyList(),
        Collections.<NoStrictDependency>emptyList());

    testee(orgExamples, Optional.empty()).analyseStrict(one(definition));
  }

  private static <T> List<T> one(T el) {
    return Collections.singletonList(el);
  }

  @Test
  public void analyseStrictShouldAnalyseSpecificationWithMoreModulesAndRules() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER),
        Arrays.asList(dep(MAIN, CONTROLLER)),
        Arrays.asList(noSD(CONTROLLER, MAIN))
    );
    final AnalyserModel.StrictAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseStrict(one(definition)).get(0).second;

    assertThat(actual.dependencyViolations.isEmpty()).isTrue();
    assertThat(actual.noStrictDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.metrics).isEqualTo(Arrays.asList(
        met("Main", 0, 1),
        met("Controllers", 1, 0)
    ));
  }

  @Test
  public void analyseStrictShouldDetectViolationsOnSpecification() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)),
        Arrays.asList(noSD(MAIN, FACADE))
    );

    final AnalyserModel.StrictAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseStrict(one(definition)).get(0).second;

    assertThat(actual.dependencyViolations).containsAll(Arrays.asList(
        depV("Main", "Main", Arrays.asList("Controllers", "Main"), Collections.<String>emptyList()),
        depV("Controllers", "Main", Arrays.asList("Main"), Collections.<String>emptyList())
    ));
    assertThat(actual.noStrictDependencyViolations).contains(
        noDepV("Main", "Facade")
    );
    assertThat(actual.metrics).isEqualTo(Arrays.asList(
        met("Main", 0, 2),
        met("Controllers", 1, 1),
        met("Facade", 2, 0)
    ));
  }

  @Test
  public void analyseStrictShouldProvideEvidencesForDependencyViolations() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Collections.emptyList(),
        Collections.emptyList()
    );

    final AnalyserModel.StrictAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseStrict(one(definition)).get(0).second;

    assertThat(actual.dependencyViolations).containsAll(Arrays.asList(
        depV("Main", "Controllers", Collections.emptyList(), Arrays.asList("Controllers"), Arrays.asList(Arrays.asList(
            Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
            Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
            Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")
        ))),
        depV("Main", "Facade", Collections.emptyList(), Arrays.asList("Facade"), Arrays.asList(Arrays.asList(
            Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
            Pair.make("org.example.Main:main", "org.example.core.CoreFacade")
        ))),
        depV("Controllers", "Facade", Collections.emptyList(), Arrays.asList("Facade"), Arrays.asList(Arrays.asList(
            Pair.make("org.example.controller.Controller1:access", "org.example.core.CoreFacade:facadeMethod1"),
            Pair.make("org.example.controller.Controller1", "org.example.core.CoreFacade"),
            Pair.make("org.example.controller.Controller1:(init)", "org.example.core.CoreFacade")
        )))
    ));
  }

  @Test
  public void analyseStrictShouldProvideLimitedDependenciesIfEvidenceLimitConfigured() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Collections.emptyList(),
        Collections.emptyList()
    );

    final AnalyserModel.StrictAnalysisResult actual = testee(orgExamples, Optional.of(1)).analyseStrict(one(definition)).get(0).second;
    final AnalyserModel.DependencyViolation mainControllers = actual.dependencyViolations
        .stream()
        .filter(v -> v.sourceModule.equals("Main") && v.destinationModule.equals("Controllers"))
        .findFirst().get();

    assertThat(Arrays.asList(
        Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
        Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
        Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")
    )).containsAll(mainControllers.evidences.get(0));
    assertThat(mainControllers.evidences.get(0).size()).isEqualTo(1);

    final AnalyserModel.DependencyViolation mainFacade = actual.dependencyViolations
        .stream()
        .filter(v -> v.sourceModule.equals("Main") && v.destinationModule.equals("Facade"))
        .findFirst().get();
    assertThat(Arrays.asList(
        Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
        Pair.make("org.example.Main:main", "org.example.core.CoreFacade")
    )).containsAll(mainFacade.evidences.get(0));
    assertThat(mainFacade.evidences.get(0).size()).isEqualTo(1);

    final AnalyserModel.DependencyViolation controllersFacade = actual.dependencyViolations
        .stream()
        .filter(v -> v.sourceModule.equals("Controllers") && v.destinationModule.equals("Facade"))
        .findFirst().get();
    assertThat(Arrays.asList(
        Pair.make("org.example.controller.Controller1:access", "org.example.core.CoreFacade:facadeMethod1"),
        Pair.make("org.example.controller.Controller1", "org.example.core.CoreFacade"),
        Pair.make("org.example.controller.Controller1:(init)", "org.example.core.CoreFacade")
    )).containsAll(controllersFacade.evidences.get(0));
    assertThat(controllersFacade.evidences.get(0).size()).isEqualTo(1);
  }

  @Test
  public void analyseStrictShouldAnalyseAllModulesInScope() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI), dep(MAIN, IO),
            dep(CONTROLLER, FACADE),
            dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
            dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL),
            dep(COREAPI, MODEL),
            dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS)
        ),
        Arrays.asList(
            noSD(CONTROLLER, COREINTERNALS),
            noSD(MAIN, COREINTERNALS),
            noSD(IO, COREINTERNALS)
        )
    );

    final AnalyserModel.StrictAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseStrict(one(definition)).get(0).second;
    assertThat(actual.dependencyViolations.isEmpty()).isTrue();
    assertThat(actual.noStrictDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.metrics).containsAll(Arrays.asList(
        met(MAIN.name, 0, 4),
        met(CONTROLLER.name, 1, 1),
        met(FACADE.name, 2, 3),
        met(COREAPI.name, 3, 1),
        met(COREINTERNALS.name, 1, 2),
        met(IO.name, 1, 3),
        met(MODEL.name, 4, 0),
        met(UTILS.name, 2, 0)
    ));
  }

  @Test
  public void analyseStrictShouldAnalyseMultipleDefinitionsWithOnePass() throws IOException {
    final Definition definition1 = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN)),
        Arrays.asList(noSD(MAIN, FACADE))
    );

    final Definition definition2 = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI), dep(MAIN, IO),
            dep(CONTROLLER, FACADE),
            dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
            dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL),
            dep(COREAPI, MODEL),
            dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS)
        ),
        Arrays.asList(
            noSD(CONTROLLER, COREINTERNALS),
            noSD(MAIN, COREINTERNALS),
            noSD(IO, COREINTERNALS)
        )
    );

    final List<Pair<Definition, AnalyserModel.StrictAnalysisResult>> results =
        testee(orgExamples, Optional.empty())
            .analyseStrict(Arrays.asList(definition1, definition2)
            );
    final AnalyserModel.StrictAnalysisResult actual1 = results.stream().filter(p -> p.first == definition1).findFirst().get().second;
    final AnalyserModel.StrictAnalysisResult actual2 = results.stream().filter(p -> p.first == definition2).findFirst().get().second;

    verify(classParser, times(1)).parse(eq(orgExamples), any());

    assertThat(actual1.dependencyViolations).containsAll(Arrays.asList(
        depV("Main", "Main", Arrays.asList("Controllers", "Main"), Collections.<String>emptyList()),
        depV("Controllers", "Main", Arrays.asList("Main"), Collections.<String>emptyList())
    ));
    assertThat(actual1.noStrictDependencyViolations).contains(
        noDepV("Main", "Facade")
    );
    assertThat(actual1.metrics).isEqualTo(Arrays.asList(
        met("Main", 0, 2),
        met("Controllers", 1, 1),
        met("Facade", 2, 0)
    ));
    assertThat(actual2.dependencyViolations.isEmpty()).isTrue();
    assertThat(actual2.noStrictDependencyViolations.isEmpty()).isTrue();
    assertThat(actual2.metrics).containsAll(Arrays.asList(
        met(MAIN.name, 0, 4),
        met(CONTROLLER.name, 1, 1),
        met(FACADE.name, 2, 3),
        met(COREAPI.name, 3, 1),
        met(COREINTERNALS.name, 1, 2),
        met(IO.name, 1, 3),
        met(MODEL.name, 4, 0),
        met(UTILS.name, 2, 0)
    ));
  }

  private static Dependency dep(HWModule source, HWModule dest) {
    return new Dependency(source, dest);
  }

  private static NoStrictDependency noSD(HWModule source, HWModule dest) {
    return new NoStrictDependency(source, dest);
  }

  private static AnalyserModel.Metrics met(String name, int fanIn, int fanOut) {
    return new AnalyserModel.Metrics(name, fanIn, fanOut);
  }

  private static AnalyserModel.DependencyViolation depV(String source, String dest, List<String> specPath,
                                                        List<String> actualPath) {
    return new AnalyserModel.DependencyViolation(source, dest, specPath, actualPath);
  }


  private static AnalyserModel.DependencyViolation depV(String source, String dest, List<String> specPath,
                                                        List<String> actualPath, List<List<Pair<String, String>>> evidences) {
    return new AnalyserModel.DependencyViolation(source, dest, specPath, actualPath, evidences);
  }

  private static AnalyserModel.NoStrictDependencyViolation noDepV(String source, String dest) {
    return new AnalyserModel.NoStrictDependencyViolation(source, dest);
  }

  @Test
  public void analyseLooseShouldAnalyseIfSpecificationIncludesOnlyOneModuleAndNoRules() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN),
        Collections.<Dependency>emptyList(),
        Collections.<NoStrictDependency>emptyList()
    );

    final AnalyserModel.LooseAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseLoose(one(definition)).get(0).second;

    assertThat(actual.absentDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.undesiredDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.metrics).isEqualTo(Arrays.asList(met("Main", 0, 0)));
  }

  @Test(expected = AnalyserException.class)
  public void analyseLooseShouldFailIfNoModuleIsProvided() {
    final Definition definition = new Definition(Collections.<HWModule>emptyList(), Collections.<Dependency>emptyList(),
        Collections.<NoStrictDependency>emptyList());

    testee(orgExamples, Optional.empty()).analyseStrict(one(definition));
  }

  @Test
  public void analyseLooseShouldAnalyseSpecificationWithMoreModulesAndRules() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER),
        Arrays.asList(dep(MAIN, CONTROLLER)),
        Arrays.asList(noSD(CONTROLLER, MAIN))
    );
    final AnalyserModel.LooseAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseLoose(one(definition)).get(0).second;

    assertThat(actual.absentDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.undesiredDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.metrics).isEqualTo(Arrays.asList(
        met("Main", 0, 1),
        met("Controllers", 1, 0)
    ));
  }

  @Test
  public void analyseLooseShouldDetectViolationsOnSpecification() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)),
        Arrays.asList(noSD(MAIN, FACADE))
    );

    final AnalyserModel.LooseAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseLoose(one(definition)).get(0).second;

    assertThat(actual.absentDependencyViolations).containsAll(Arrays.asList(
        aDep("Controllers", "Main")
    ));
    assertThat(actual.undesiredDependencyViolations).contains(
        unDep("Main", "Facade", Arrays.asList("Facade"), Arrays.asList(Arrays.asList(
            Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
            Pair.make("org.example.Main:main", "org.example.core.CoreFacade")
        )))
    );
    assertThat(actual.metrics).isEqualTo(Arrays.asList(
        met("Main", 0, 2),
        met("Controllers", 1, 1),
        met("Facade", 2, 0)
    ));
  }

  @Test
  public void analyseLooseShouldProvideEvidenceForUndesiredDependency() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Collections.emptyList(),
        Arrays.asList(noSD(MAIN, CONTROLLER))
    );

    final AnalyserModel.LooseAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseLoose(one(definition)).get(0).second;

    assertThat(actual.undesiredDependencyViolations).contains(
        unDep("Main", "Controllers", Arrays.asList("Controllers"), Arrays.asList(Arrays.asList(
            Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
            Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
            Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")
        )))
    );
  }

  @Test
  public void analyseLooseShouldProvideLimitedDependenciesIfEvidenceLimitConfigured() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Collections.emptyList(),
        Arrays.asList(noSD(MAIN, CONTROLLER))
    );

    final AnalyserModel.LooseAnalysisResult actual = testee(orgExamples, Optional.of(1)).analyseLoose(one(definition)).get(0).second;

    final AnalyserModel.UndesiredDependencyViolation mainController = actual.undesiredDependencyViolations.stream()
        .filter(v -> v.sourceModule.equals("Main") && v.destinationModule.equals("Controllers"))
        .findFirst().get();

    assertThat(Arrays.asList(
        Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
        Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
        Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")
    )).containsAll(mainController.evidences.get(0));
    assertThat(mainController.evidences.get(0).size()).isEqualTo(1);
  }

  private static AnalyserModel.AbsentDependencyViolation aDep(String source, String dest) {
    return new AnalyserModel.AbsentDependencyViolation(source, dest);
  }

  private static AnalyserModel.UndesiredDependencyViolation unDep(String source, String dest, List<String> evidence, List<List<Pair<String, String>>> evidencePath) {
    return new AnalyserModel.UndesiredDependencyViolation(source, dest, evidence, evidencePath);
  }

  @Test
  public void analyseLoseShouldAnalyseAllModulesInScope() {
    final Definition definition = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, IO), dep(MAIN, MODEL),
            dep(CONTROLLER, FACADE),
            dep(COREINTERNALS, MODEL),
            dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
            dep(COREAPI, MODEL),
            dep(IO, COREAPI), dep(IO, MODEL)
        ),
        Arrays.asList(
            noSD(IO, COREINTERNALS),
            noSD(UTILS, MAIN)
        )
    );

    final AnalyserModel.LooseAnalysisResult actual = testee(orgExamples, Optional.empty()).analyseLoose(one(definition)).get(0).second;
    assertThat(actual.absentDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.undesiredDependencyViolations.isEmpty()).isTrue();
    assertThat(actual.metrics).containsAll(Arrays.asList(
        met(MAIN.name, 0, 4),
        met(CONTROLLER.name, 1, 1),
        met(FACADE.name, 2, 3),
        met(COREAPI.name, 3, 1),
        met(COREINTERNALS.name, 1, 2),
        met(IO.name, 1, 3),
        met(MODEL.name, 4, 0),
        met(UTILS.name, 2, 0)
    ));
  }

  @Test
  public void analyseLooseShouldAnalyseMultipleDefinitionsWithOnePass() throws IOException {
    final Definition definition1 = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN)),
        Arrays.asList(noSD(MAIN, FACADE))
    );
    final Definition definition2 = new Definition(
        Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL),
        Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, IO), dep(MAIN, MODEL),
            dep(CONTROLLER, FACADE),
            dep(COREINTERNALS, MODEL),
            dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
            dep(COREAPI, MODEL),
            dep(IO, COREAPI), dep(IO, MODEL)
        ),
        Arrays.asList(
            noSD(IO, COREINTERNALS),
            noSD(UTILS, MAIN)
        )
    );

    final List<Pair<Definition, AnalyserModel.LooseAnalysisResult>> results =
        testee(orgExamples, Optional.empty())
            .analyseLoose(Arrays.asList(definition1, definition2)
            );
    final AnalyserModel.LooseAnalysisResult actual1 = results.stream().filter(p -> p.first == definition1).findFirst().get().second;
    final AnalyserModel.LooseAnalysisResult actual2 = results.stream().filter(p -> p.first == definition2).findFirst().get().second;

    verify(classParser, times(1)).parse(eq(orgExamples), any());

    assertThat(actual1.absentDependencyViolations).containsAll(Arrays.asList(
        aDep("Controllers", "Main")
    ));
    assertThat(actual1.undesiredDependencyViolations).contains(
        unDep("Main", "Facade", Arrays.asList("Facade"), Arrays.asList(Arrays.asList(
            Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
            Pair.make("org.example.Main:main", "org.example.core.CoreFacade")
        )))
    );
    assertThat(actual1.metrics).isEqualTo(Arrays.asList(
        met("Main", 0, 2),
        met("Controllers", 1, 1),
        met("Facade", 2, 0)
    ));
    assertThat(actual2.absentDependencyViolations.isEmpty()).isTrue();
    assertThat(actual2.undesiredDependencyViolations.isEmpty()).isTrue();
    assertThat(actual2.metrics).containsAll(Arrays.asList(
        met(MAIN.name, 0, 4),
        met(CONTROLLER.name, 1, 1),
        met(FACADE.name, 2, 3),
        met(COREAPI.name, 3, 1),
        met(COREINTERNALS.name, 1, 2),
        met(IO.name, 1, 3),
        met(MODEL.name, 4, 0),
        met(UTILS.name, 2, 0)
    ));
  }
}
