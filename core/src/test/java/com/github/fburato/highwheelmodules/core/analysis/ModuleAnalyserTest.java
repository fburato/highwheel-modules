package com.github.fburato.highwheelmodules.core.analysis;

import com.github.fburato.highwheelmodules.core.externaladapters.GuavaGraphFactory;
import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode;
import com.github.fburato.highwheelmodules.model.modules.AnonymousModule;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraphFactory;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;
import com.github.fburato.highwheelmodules.utils.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.fburato.highwheelmodules.bytecodeparser.ClassPathParser;
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.DirectoryClassPathRoot;
import com.github.fburato.highwheelmodules.model.classpath.ClassParser;
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("ModuleAnalyser")
class ModuleAnalyserTest {

    private final ClasspathRoot orgExamples = new DirectoryClassPathRoot(
            new File(join(File.separatorChar + "", Arrays.asList("target", "test-classes"))));
    private final Predicate<ElementName> matchOnlyExampleDotOrg = item -> item.asJavaName().startsWith("org.example");

    private final ClassParser realClassParser = new ClassPathParser(matchOnlyExampleDotOrg);
    private final ClassParser classParser = spy(realClassParser);
    private final ModuleGraphFactory factory = new GuavaGraphFactory();

    private final HWModule MAIN = HWModule.make("Main", "org.example.Main").get();
    private final HWModule CONTROLLER = HWModule.make("Controllers", "org.example.controller.*").get();
    private final HWModule FACADE = HWModule.make("Facade", "org.example.core.CoreFacade").get();
    private final HWModule COREINTERNALS = HWModule.make("CoreInternals", "org.example.core.internals.*").get();
    private final HWModule COREAPI = HWModule.make("CoreApi", "org.example.core.api.*").get();
    private final HWModule MODEL = HWModule.make("Model", "org.example.core.model.*").get();
    private final HWModule IO = HWModule.make("IO", "org.example.io.*").get();
    private final HWModule UTILS = HWModule.make("Commons", "org.example.commons.*").get();

    private ModuleAnalyserS testee(ClasspathRoot root, Optional<Integer> evidenceLimit) {
        return ModuleAnalyserS.apply(classParser, root, evidenceLimit, factory);
    }

    @Nested
    @DisplayName("when definitions are all strict, analyse should")
    class StrictTests {
        final Definition.DefinitionBuilder strictBuilder = Definition.DefinitionBuilder.baseBuilder()
                .with($ -> $.mode = AnalysisMode.STRICT);

        @Test
        @DisplayName("analyse if specification includes only one module and no rules")
        void testOneModuleNoRule() {
            final Definition definition = strictBuilder.with($ -> $.modules = Collections.singletonList(MAIN)).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).isEqualTo(Collections.singletonList(met("Main", 0, 0)));
        }

        @Test
        @DisplayName("fail if no module is provided")
        void testNoModule() {
            assertThrows(AnalyserException.class, () -> {
                final Definition definition = strictBuilder.build();

                testee(orgExamples, Optional.empty()).analyse(one(definition));
            });
        }

        @Test
        @DisplayName("analyse specification with many modules and rules")
        void testManyModulesAndRules() {
            final Definition definition = strictBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER);
                $.dependencies = Collections.singletonList(dep(MAIN, CONTROLLER));
                $.noStrictDependencies = Collections.singletonList(noSD(CONTROLLER, MAIN));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).isEqualTo(Arrays.asList(met("Main", 0, 1), met("Controllers", 1, 0)));
        }

        @Test
        @DisplayName("detect violations on specification")
        void testDetectViolation() {
            final Definition definition = strictBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN));
                $.noStrictDependencies = Collections.singletonList(noSD(MAIN, FACADE));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations).containsAll(
                    Arrays.asList(depV("Main", "Main", Arrays.asList("Controllers", "Main"), Collections.emptyList()),
                            depV("Controllers", "Main", Collections.singletonList("Main"), Collections.emptyList())));
            assertThat(actual.moduleConnectionViolations).contains(noDepV("Main", "Facade"));
            assertThat(actual.metrics)
                    .isEqualTo(Arrays.asList(met("Main", 0, 2), met("Controllers", 1, 1), met("Facade", 2, 0)));
        }

        @Test
        @DisplayName("provide evidence for dependency violations")
        void testEvidence() {
            final Definition definition = strictBuilder.with($ -> $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE))
                    .build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations)
                    .containsAll(Arrays.asList(
                            depV("Main", "Controllers", Collections.emptyList(),
                                    Collections.singletonList("Controllers"),
                                    Collections.singletonList(Arrays.asList(Pair.make("org.example.Main:main",
                                            "org.example.controller.Controller1:access"),
                                            Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
                                            Pair.make("org.example.Main:main",
                                                    "org.example.controller.Controller1:(init)")))),
                            depV("Main", "Facade", Collections.emptyList(), Collections.singletonList("Facade"),
                                    Collections.singletonList(Arrays.asList(
                                            Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
                                            Pair.make("org.example.Main:main", "org.example.core.CoreFacade")))),
                            depV("Controllers", "Facade", Collections.emptyList(), Collections.singletonList("Facade"),
                                    Collections.singletonList(Arrays.asList(
                                            Pair.make("org.example.controller.Controller1:access",
                                                    "org.example.core.CoreFacade:facadeMethod1"),
                                            Pair.make("org.example.controller.Controller1",
                                                    "org.example.core.CoreFacade"),
                                            Pair.make("org.example.controller.Controller1:(init)",
                                                    "org.example.core.CoreFacade"))))));
        }

        @Test
        @DisplayName("limit evidence for dependency violations if evidence limit configured")
        void analyseStrictShouldProvideLimitedDependenciesIfEvidenceLimitConfigured() {
            final Definition definition = strictBuilder.with($ -> $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE))
                    .build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.of(1)).analyse(one(definition))
                    .get(0);
            final AnalyserModel.EvidenceBackedViolation mainControllers = actual.evidenceBackedViolations.stream()
                    .filter(v -> v.sourceModule.equals("Main") && v.destinationModule.equals("Controllers")).findFirst()
                    .get();

            assertThat(Arrays.asList(Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
                    Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
                    Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")))
                            .containsAll(mainControllers.evidences.get(0));
            assertThat(mainControllers.evidences.get(0).size()).isEqualTo(1);

            final AnalyserModel.EvidenceBackedViolation mainFacade = actual.evidenceBackedViolations.stream()
                    .filter(v -> v.sourceModule.equals("Main") && v.destinationModule.equals("Facade")).findFirst()
                    .get();
            assertThat(Arrays.asList(Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
                    Pair.make("org.example.Main:main", "org.example.core.CoreFacade")))
                            .containsAll(mainFacade.evidences.get(0));
            assertThat(mainFacade.evidences.get(0).size()).isEqualTo(1);

            final AnalyserModel.EvidenceBackedViolation controllersFacade = actual.evidenceBackedViolations.stream()
                    .filter(v -> v.sourceModule.equals("Controllers") && v.destinationModule.equals("Facade"))
                    .findFirst().get();
            assertThat(Arrays.asList(
                    Pair.make("org.example.controller.Controller1:access", "org.example.core.CoreFacade:facadeMethod1"),
                    Pair.make("org.example.controller.Controller1", "org.example.core.CoreFacade"),
                    Pair.make("org.example.controller.Controller1:(init)", "org.example.core.CoreFacade")))
                            .containsAll(controllersFacade.evidences.get(0));
            assertThat(controllersFacade.evidences.get(0).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("take all modules in scope")
        void analyseStrictShouldAnalyseAllModulesInScope() {
            final Definition definition = strictBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI),
                        dep(MAIN, IO), dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
                        dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL), dep(COREAPI, MODEL),
                        dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS));
                $.noStrictDependencies = Arrays.asList(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS),
                        noSD(IO, COREINTERNALS));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 4), met(CONTROLLER.name, 1, 1),
                    met(FACADE.name, 2, 3), met(COREAPI.name, 3, 1), met(COREINTERNALS.name, 1, 2), met(IO.name, 1, 3),
                    met(MODEL.name, 4, 0), met(UTILS.name, 2, 0)));
        }

        @Test
        @DisplayName("analyseStrict should consider only dependencies in the whitelist")
        void testStrictWhiteList() {
            final Definition definition = strictBuilder.with($ -> {
                $.whitelist = AnonymousModule.make("org.example.Main", "org.example.controller.*");
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Collections.singletonList(dep(MAIN, CONTROLLER));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 1), met(CONTROLLER.name, 1, 0),
                    met(FACADE.name, 0, 0), met(COREAPI.name, 0, 0), met(COREINTERNALS.name, 0, 0), met(IO.name, 0, 0),
                    met(MODEL.name, 0, 0), met(UTILS.name, 0, 0)));
        }

        @Test
        @DisplayName("analyseStrict should consider dependencies not in the blacklist")
        void testStrictBlackList() {
            final Definition definition = strictBuilder.with($ -> {
                $.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*");
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL),
                        dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL), dep(COREAPI, MODEL),
                        dep(IO, COREAPI), dep(IO, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(CONTROLLER, COREINTERNALS), noSD(IO, COREINTERNALS));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 0), met(CONTROLLER.name, 0, 1),
                    met(FACADE.name, 1, 3), met(COREAPI.name, 2, 1), met(COREINTERNALS.name, 1, 1), met(IO.name, 0, 2),
                    met(MODEL.name, 4, 0), met(UTILS.name, 0, 0)));
        }

        @Test
        @DisplayName("analyseStrict should consider dependencies not in the blacklist and in the whitelist")
        void testStrictWhiteBlackList() {
            final Definition definition = strictBuilder.with($ -> {
                $.whitelist = AnonymousModule.make("org.example.*");
                $.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*");
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL),
                        dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL), dep(COREAPI, MODEL),
                        dep(IO, COREAPI), dep(IO, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(CONTROLLER, COREINTERNALS), noSD(IO, COREINTERNALS));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 0), met(CONTROLLER.name, 0, 1),
                    met(FACADE.name, 1, 3), met(COREAPI.name, 2, 1), met(COREINTERNALS.name, 1, 1), met(IO.name, 0, 2),
                    met(MODEL.name, 4, 0), met(UTILS.name, 0, 0)));
        }

        @Test
        @DisplayName("analyse multiple definition with one pass")
        void testManyWithOnePass() throws IOException {
            final Definition definition1 = strictBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN));
                $.noStrictDependencies = Collections.singletonList(noSD(MAIN, FACADE));
            }).build();

            final Definition definition2 = strictBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI),
                        dep(MAIN, IO), dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
                        dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL), dep(COREAPI, MODEL),
                        dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS));
                $.noStrictDependencies = Arrays.asList(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS),
                        noSD(IO, COREINTERNALS));
            }).build();

            final List<AnalyserModel.AnalysisResult> results = testee(orgExamples, Optional.empty())
                    .analyse(Arrays.asList(definition1, definition2));
            final AnalyserModel.AnalysisResult actual1 = results.get(0);
            final AnalyserModel.AnalysisResult actual2 = results.get(1);

            verify(classParser, times(1)).parse(eq(orgExamples), any());

            assertThat(actual1.evidenceBackedViolations).containsAll(
                    Arrays.asList(depV("Main", "Main", Arrays.asList("Controllers", "Main"), Collections.emptyList()),
                            depV("Controllers", "Main", Collections.singletonList("Main"), Collections.emptyList())));
            assertThat(actual1.moduleConnectionViolations).contains(noDepV("Main", "Facade"));
            assertThat(actual1.metrics)
                    .isEqualTo(Arrays.asList(met("Main", 0, 2), met("Controllers", 1, 1), met("Facade", 2, 0)));
            assertThat(actual2.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual2.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual2.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 4), met(CONTROLLER.name, 1, 1),
                    met(FACADE.name, 2, 3), met(COREAPI.name, 3, 1), met(COREINTERNALS.name, 1, 2), met(IO.name, 1, 3),
                    met(MODEL.name, 4, 0), met(UTILS.name, 2, 0)));
        }
    }

    private static <T> List<T> one(T el) {
        return Collections.singletonList(el);
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

    private static AnalyserModel.EvidenceBackedViolation depV(String source, String dest, List<String> specPath,
            List<String> actualPath) {
        return new AnalyserModel.EvidenceBackedViolation(source, dest, specPath, actualPath, Collections.emptyList());
    }

    private static AnalyserModel.EvidenceBackedViolation depV(String source, String dest, List<String> specPath,
            List<String> actualPath, List<List<Pair<String, String>>> evidences) {
        return new AnalyserModel.EvidenceBackedViolation(source, dest, specPath, actualPath, evidences);
    }

    private static AnalyserModel.ModuleConnectionViolation noDepV(String source, String dest) {
        return new AnalyserModel.ModuleConnectionViolation(source, dest);
    }

    @Nested
    @DisplayName("when definitions are all strict, analyse should")
    class LooseTests {
        final Definition.DefinitionBuilder looseBuilder = Definition.DefinitionBuilder.baseBuilder()
                .with($ -> $.mode = AnalysisMode.LOOSE);

        @Test
        @DisplayName("analyse if specification has one module and no rule")
        void testOneModule() {
            final Definition definition = looseBuilder.with($ -> $.modules = Collections.singletonList(MAIN)).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).isEqualTo(Collections.singletonList(met("Main", 0, 0)));
        }

        @Test
        @DisplayName("fail if no module is provided")
        void testFailNoModule() {
            assertThrows(AnalyserException.class, () -> {
                final Definition definition = looseBuilder.build();

                testee(orgExamples, Optional.empty()).analyse(one(definition));
            });
        }

        @Test
        @DisplayName("analyse specification with many modules and rules")
        void testManyModulesAndRules() {
            final Definition definition = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER);
                $.dependencies = Collections.singletonList(dep(MAIN, CONTROLLER));
                $.noStrictDependencies = Collections.singletonList(noSD(CONTROLLER, MAIN));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).isEqualTo(Arrays.asList(met("Main", 0, 1), met("Controllers", 1, 0)));
        }

        @Test
        @DisplayName("detect violations on the specification")
        void testViolationDetection() {
            final Definition definition = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN));
                $.noStrictDependencies = Collections.singletonList(noSD(MAIN, FACADE));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.moduleConnectionViolations)
                    .containsAll(Collections.singletonList(aDep("Controllers", "Main")));
            assertThat(actual.evidenceBackedViolations)
                    .contains(unDep("Main", "Facade", Collections.singletonList("Facade"),
                            Collections.singletonList(Arrays.asList(
                                    Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
                                    Pair.make("org.example.Main:main", "org.example.core.CoreFacade")))));
            assertThat(actual.metrics)
                    .isEqualTo(Arrays.asList(met("Main", 0, 2), met("Controllers", 1, 1), met("Facade", 2, 0)));
        }

        @Test
        @DisplayName("provide evidence for undesired dependencies")
        void testEvidenceForUndesired() {
            final Definition definition = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
                $.noStrictDependencies = Collections.singletonList(noSD(MAIN, CONTROLLER));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.evidenceBackedViolations)
                    .contains(unDep("Main", "Controllers", Collections.singletonList("Controllers"),
                            Collections.singletonList(Arrays.asList(
                                    Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
                                    Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
                                    Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")))));
        }

        @Test
        @DisplayName("limit the evidence provided if limit configured")
        void testEvidenceLimit() {
            final Definition definition = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
                $.noStrictDependencies = Collections.singletonList(noSD(MAIN, CONTROLLER));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.of(1)).analyse(one(definition))
                    .get(0);

            final AnalyserModel.EvidenceBackedViolation mainController = actual.evidenceBackedViolations.stream()
                    .filter(v -> v.sourceModule.equals("Main") && v.destinationModule.equals("Controllers")).findFirst()
                    .get();

            assertThat(Arrays.asList(Pair.make("org.example.Main:main", "org.example.controller.Controller1:access"),
                    Pair.make("org.example.Main:main", "org.example.controller.Controller1"),
                    Pair.make("org.example.Main:main", "org.example.controller.Controller1:(init)")))
                            .containsAll(mainController.evidences.get(0));
            assertThat(mainController.evidences.get(0).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("analyse all modules in scope")
        void analyseLoseShouldAnalyseAllModulesInScope() {
            final Definition definition = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, IO), dep(MAIN, MODEL),
                        dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL), dep(FACADE, COREINTERNALS),
                        dep(FACADE, MODEL), dep(COREAPI, MODEL), dep(IO, COREAPI), dep(IO, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);

            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 4), met(CONTROLLER.name, 1, 1),
                    met(FACADE.name, 2, 3), met(COREAPI.name, 3, 1), met(COREINTERNALS.name, 1, 2), met(IO.name, 1, 3),
                    met(MODEL.name, 4, 0), met(UTILS.name, 2, 0)));
        }

        @Test
        @DisplayName("analyseLoose should consider only dependencies in the whitelist")
        void testAnalyseLooseWhiteList() {
            final Definition definition = looseBuilder.with($ -> {
                $.whitelist = AnonymousModule.make("org.example.controller.*", "org.example.core.CoreFacade",
                        "org.example.core.model.*");
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(CONTROLLER, FACADE), dep(FACADE, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 0), met(CONTROLLER.name, 0, 1),
                    met(FACADE.name, 1, 1), met(COREAPI.name, 0, 0), met(COREINTERNALS.name, 0, 0), met(IO.name, 0, 0),
                    met(MODEL.name, 1, 0), met(UTILS.name, 0, 0)));
        }

        @Test
        @DisplayName("analyseLoose should consider dependencies not in the blacklist")
        void testAnalyseLooseBlackList() {
            final Definition definition = looseBuilder.with($ -> {
                $.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*");
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL),
                        dep(FACADE, COREINTERNALS), dep(FACADE, MODEL), dep(COREAPI, MODEL), dep(IO, COREAPI),
                        dep(IO, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 0), met(CONTROLLER.name, 0, 1),
                    met(FACADE.name, 1, 3), met(COREAPI.name, 2, 1), met(COREINTERNALS.name, 1, 1), met(IO.name, 0, 2),
                    met(MODEL.name, 4, 0), met(UTILS.name, 0, 0)));
        }

        @Test
        @DisplayName("analyseLoose should consider dependencies in the whitelist and not in the blacklist")
        void testAnalyseLooseWhiteBlackList() {
            final Definition definition = looseBuilder.with($ -> {
                $.whitelist = AnonymousModule.make("org.example.*");
                $.blackList = AnonymousModule.make("org.example.Main", "org.example.commons.*");
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL),
                        dep(FACADE, COREINTERNALS), dep(FACADE, MODEL), dep(COREAPI, MODEL), dep(IO, COREAPI),
                        dep(IO, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN));
            }).build();

            final AnalyserModel.AnalysisResult actual = testee(orgExamples, Optional.empty()).analyse(one(definition))
                    .get(0);
            assertThat(actual.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 0), met(CONTROLLER.name, 0, 1),
                    met(FACADE.name, 1, 3), met(COREAPI.name, 2, 1), met(COREINTERNALS.name, 1, 1), met(IO.name, 0, 2),
                    met(MODEL.name, 4, 0), met(UTILS.name, 0, 0)));
        }

        @Test
        @DisplayName("analyse multiple definitions in one pass")
        void testOnePass() throws IOException {
            final Definition definition1 = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN));
                $.noStrictDependencies = Collections.singletonList(noSD(MAIN, FACADE));
            }).build();

            final Definition definition2 = looseBuilder.with($ -> {
                $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
                $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, IO), dep(MAIN, MODEL),
                        dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL), dep(FACADE, COREINTERNALS),
                        dep(FACADE, MODEL), dep(COREAPI, MODEL), dep(IO, COREAPI), dep(IO, MODEL));
                $.noStrictDependencies = Arrays.asList(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN));
            }).build();

            final List<AnalyserModel.AnalysisResult> results = testee(orgExamples, Optional.empty())
                    .analyse(Arrays.asList(definition1, definition2));
            final AnalyserModel.AnalysisResult actual1 = results.get(0);
            final AnalyserModel.AnalysisResult actual2 = results.get(1);

            verify(classParser, times(1)).parse(eq(orgExamples), any());

            assertThat(actual1.moduleConnectionViolations)
                    .containsAll(Collections.singletonList(aDep("Controllers", "Main")));
            assertThat(actual1.evidenceBackedViolations)
                    .contains(unDep("Main", "Facade", Collections.singletonList("Facade"),
                            Collections.singletonList(Arrays.asList(
                                    Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
                                    Pair.make("org.example.Main:main", "org.example.core.CoreFacade")))));
            assertThat(actual1.metrics)
                    .isEqualTo(Arrays.asList(met("Main", 0, 2), met("Controllers", 1, 1), met("Facade", 2, 0)));
            assertThat(actual2.moduleConnectionViolations.isEmpty()).isTrue();
            assertThat(actual2.evidenceBackedViolations.isEmpty()).isTrue();
            assertThat(actual2.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 4), met(CONTROLLER.name, 1, 1),
                    met(FACADE.name, 2, 3), met(COREAPI.name, 3, 1), met(COREINTERNALS.name, 1, 2), met(IO.name, 1, 3),
                    met(MODEL.name, 4, 0), met(UTILS.name, 2, 0)));
        }
    }

    private static AnalyserModel.ModuleConnectionViolation aDep(String source, String dest) {
        return new AnalyserModel.ModuleConnectionViolation(source, dest);
    }

    private static AnalyserModel.EvidenceBackedViolation unDep(String source, String dest, List<String> evidence,
            List<List<Pair<String, String>>> evidencePath) {
        return new AnalyserModel.EvidenceBackedViolation(source, dest, Arrays.asList(source, dest), evidence,
                evidencePath);
    }

    @Test
    @DisplayName("analyse should change mode based on the definition")
    void testMultiMode() throws IOException {
        final Definition definition1 = Definition.DefinitionBuilder.baseBuilder().with($ -> {
            $.mode = AnalysisMode.STRICT;
            $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
            $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, FACADE), dep(CONTROLLER, MAIN));
            $.noStrictDependencies = Collections.singletonList(noSD(MAIN, FACADE));
        }).build();

        final Definition definition2 = Definition.DefinitionBuilder.baseBuilder().with($ -> {
            $.mode = AnalysisMode.STRICT;
            $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
            $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, FACADE), dep(MAIN, COREAPI), dep(MAIN, IO),
                    dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL), dep(COREINTERNALS, UTILS),
                    dep(FACADE, COREINTERNALS), dep(FACADE, COREAPI), dep(FACADE, MODEL), dep(COREAPI, MODEL),
                    dep(IO, COREAPI), dep(IO, MODEL), dep(IO, UTILS));
            $.noStrictDependencies = Arrays.asList(noSD(CONTROLLER, COREINTERNALS), noSD(MAIN, COREINTERNALS),
                    noSD(IO, COREINTERNALS));
        }).build();
        final Definition definition3 = Definition.DefinitionBuilder.baseBuilder().with($ -> {
            $.mode = AnalysisMode.LOOSE;
            $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE);
            $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(CONTROLLER, MAIN));
            $.noStrictDependencies = Collections.singletonList(noSD(MAIN, FACADE));
        }).build();

        final Definition definition4 = Definition.DefinitionBuilder.baseBuilder().with($ -> {
            $.mode = AnalysisMode.LOOSE;
            $.modules = Arrays.asList(MAIN, CONTROLLER, FACADE, COREINTERNALS, IO, COREAPI, UTILS, MODEL);
            $.dependencies = Arrays.asList(dep(MAIN, CONTROLLER), dep(MAIN, IO), dep(MAIN, MODEL),
                    dep(CONTROLLER, FACADE), dep(COREINTERNALS, MODEL), dep(FACADE, COREINTERNALS), dep(FACADE, MODEL),
                    dep(COREAPI, MODEL), dep(IO, COREAPI), dep(IO, MODEL));
            $.noStrictDependencies = Arrays.asList(noSD(IO, COREINTERNALS), noSD(UTILS, MAIN));
        }).build();

        final List<AnalyserModel.AnalysisResult> results = testee(orgExamples, Optional.empty())
                .analyse(Arrays.asList(definition1, definition2, definition3, definition4));
        final AnalyserModel.AnalysisResult actual1 = results.get(0);
        final AnalyserModel.AnalysisResult actual2 = results.get(1);
        final AnalyserModel.AnalysisResult actual3 = results.get(2);
        final AnalyserModel.AnalysisResult actual4 = results.get(3);

        verify(classParser, times(1)).parse(eq(orgExamples), any());

        assertThat(actual1.evidenceBackedViolations).containsAll(
                Arrays.asList(depV("Main", "Main", Arrays.asList("Controllers", "Main"), Collections.emptyList()),
                        depV("Controllers", "Main", Collections.singletonList("Main"), Collections.emptyList())));
        assertThat(actual1.moduleConnectionViolations).contains(noDepV("Main", "Facade"));
        assertThat(actual1.metrics)
                .isEqualTo(Arrays.asList(met("Main", 0, 2), met("Controllers", 1, 1), met("Facade", 2, 0)));
        assertThat(actual2.evidenceBackedViolations.isEmpty()).isTrue();
        assertThat(actual2.moduleConnectionViolations.isEmpty()).isTrue();
        assertThat(actual2.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 4), met(CONTROLLER.name, 1, 1),
                met(FACADE.name, 2, 3), met(COREAPI.name, 3, 1), met(COREINTERNALS.name, 1, 2), met(IO.name, 1, 3),
                met(MODEL.name, 4, 0), met(UTILS.name, 2, 0)));

        assertThat(actual3.moduleConnectionViolations)
                .containsAll(Collections.singletonList(aDep("Controllers", "Main")));
        assertThat(actual3.evidenceBackedViolations)
                .contains(unDep("Main", "Facade", Collections.singletonList("Facade"),
                        Collections.singletonList(
                                Arrays.asList(Pair.make("org.example.Main:main", "org.example.core.CoreFacade:(init)"),
                                        Pair.make("org.example.Main:main", "org.example.core.CoreFacade")))));
        assertThat(actual3.metrics)
                .isEqualTo(Arrays.asList(met("Main", 0, 2), met("Controllers", 1, 1), met("Facade", 2, 0)));
        assertThat(actual4.moduleConnectionViolations.isEmpty()).isTrue();
        assertThat(actual4.evidenceBackedViolations.isEmpty()).isTrue();
        assertThat(actual4.metrics).containsAll(Arrays.asList(met(MAIN.name, 0, 4), met(CONTROLLER.name, 1, 1),
                met(FACADE.name, 2, 3), met(COREAPI.name, 3, 1), met(COREINTERNALS.name, 1, 2), met(IO.name, 1, 3),
                met(MODEL.name, 4, 0), met(UTILS.name, 2, 0)));
    }

}
