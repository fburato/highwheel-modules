package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.externaladapters.GuavaModuleGraph;
import com.github.fburato.highwheelmodules.model.modules.AnonymousModule;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleDependency;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ModuleDependenciesGraphBuildingVisitor")
public class ModuleDependenciesGraphBuildingVisitorTest {

    private final HWModule SUPER_MODULE = HWModule.make("SuperModule", "org.example.*").get();
    private final HWModule CORE = HWModule.make("Core", "org.example.core.*").get();
    private final HWModule IO = HWModule.make("IO", "org.example.io.*").get();
    private final HWModule COMMONS = HWModule.make("Commons", "org.example.commons.*").get();
    private final HWModule ENDPOINTS = HWModule.make("Endpoints", "org.example.endpoints.*").get();
    private final HWModule MAIN = HWModule.make("Main", "org.example.Main").get();
    private final HWModule OTHER = HWModule.make("Other", "").get();

    private final List<HWModule> modules = Arrays.asList(CORE, IO, COMMONS, ENDPOINTS, MAIN);

    private static class Pair<T1, T2> {
        public final T1 first;
        public final T2 second;

        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }
    }

    private static <T1, T2> Pair<T1, T2> makePair(T1 first, T2 second) {
        return new Pair<>(first, second);
    }

    private final List<HWModule> constructionWarnings = new ArrayList<>(5);
    private final List<Pair<ElementName, Collection<HWModule>>> visitWarnings = new ArrayList<>(5);

    private class AddToListWarnings implements WarningsCollector {

        @Override
        public void constructionWarning(final HWModule m) {
            constructionWarnings.add(m);
        }

        @Override
        public void accessPointWarning(ElementName ap, Collection<HWModule> matchingModules) {
            visitWarnings.add(makePair(ap, matchingModules));
        }
    }

    private final MutableNetwork<HWModule, ModuleDependency> graph = NetworkBuilder.directed().build();
    private final GuavaModuleGraph moduleGraph = new GuavaModuleGraph(graph);
    private final WarningsCollector warningsCollector = new AddToListWarnings();
    private final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<ModuleDependency> builder = (m1, m2, source,
            dest, type) -> new ModuleDependency(m1, m2);
    private final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = new ModuleDependenciesGraphBuildingVisitor<>(
            modules, moduleGraph, OTHER, builder);

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should add all modules to the module graph")
        void testAddModules() {
            final List<HWModule> allModules = new ArrayList<>(modules.size() + 1);
            allModules.addAll(modules);
            allModules.add(OTHER);
            assertThat(graph.nodes().containsAll(allModules)).isTrue();
            assertThat(allModules.containsAll(graph.nodes())).isTrue();
        }

        @Test
        @DisplayName("should remark repeated modules")
        void testRemarkRepeated() {
            final List<HWModule> repeatedModules = Arrays.asList(HWModule.make("Core", "org.example.core.*").get(),
                    HWModule.make("Core", "org.example.io.*").get());
            final MutableNetwork<HWModule, ModuleDependency> graph = NetworkBuilder.directed().build();
            final GuavaModuleGraph moduleGraph = new GuavaModuleGraph(graph);
            final WarningsCollector warningsCollector = new AddToListWarnings();
            new ModuleDependenciesGraphBuildingVisitor<>(repeatedModules, moduleGraph, OTHER, builder,
                    warningsCollector);

            assertThat(constructionWarnings.size()).isEqualTo(1);
            assertThat(constructionWarnings.get(0).name).isEqualTo("Core");
        }
    }

    @Nested
    @DisplayName("apply")
    class ApplyTests {

        ModuleDependenciesGraphBuildingVisitor<ModuleDependency> makeTestee(Optional<AnonymousModule> whiteList,
                Optional<AnonymousModule> blackList) {
            return new ModuleDependenciesGraphBuildingVisitor<>(modules, moduleGraph, OTHER, builder, whiteList,
                    blackList);
        }

        @Test
        @DisplayName("should ignore operation if source matches black list")
        void testBlackListSource() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(Optional.empty(),
                    AnonymousModule.make("*foo*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.foo"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.FileReader"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if destination matches black list")
        void testBlackListDest() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(Optional.empty(),
                    AnonymousModule.make("*foo*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Test"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.foo"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if source does not match white list")
        void testWhiteListSource() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("*bar*"), Optional.empty());
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Test"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.bar"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if destination does not match white list")
        void testWhiteListDest() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("*bar*"), Optional.empty());
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.bar"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Test"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if source and dest match white list but source matches black list")
        void testWhiteBlackSource() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("org.example.*"), AnonymousModule.make("*bar*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.bar"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Test"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if source and dest match white list but dest matches black list")
        void testWhiteBlackDest() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("org.example.*"), AnonymousModule.make("*bar*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Test"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.bar"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if source and dest do not match black list but source does not match white list")
        void testBlackWhiteSource() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("org.example.*foo"), AnonymousModule.make("*bar*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Test"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Readerfoo"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should ignore operation if source and dest do not match black list but dest does not match white list")
        void testBlackWhiteDest() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("org.example.*foo"), AnonymousModule.make("*bar*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Testfoo"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Reader"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isEmpty();
        }

        @Test
        @DisplayName("should not ignore operation if source and dest do not match black list and match white list")
        void testBlackWhiteMatch() {
            final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee = makeTestee(
                    AnonymousModule.make("org.example.*"), AnonymousModule.make("*bar*"));
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Testfoo"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Reader"));

            testee.apply(source, dest, null);

            assertThat(moduleGraph.findDependency(CORE, IO)).isNotEmpty();
        }

        @Test
        @DisplayName("should add source and destination to the appropriate modules")
        public void testAddSourceAndDest() {
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.FileReader"));

            testee.apply(source, dest, null);

            final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(CORE, IO);

            assertThat(moduleDependency.isPresent()).isTrue();
        }

        @Test
        @DisplayName("should not add connection for elements not matching")
        public void testNotMatching() {
            final AccessPoint source = AccessPoint.create(ElementName.fromString("NOTORG.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.FileReader"));

            testee.apply(source, dest, null);

            final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(CORE, IO);

            assertThat(moduleDependency.isPresent()).isFalse();
        }

        @Test
        @DisplayName("should connect elements not matching to other")
        public void testConnectNotMatchingOther() {
            final AccessPoint source = AccessPoint.create(ElementName.fromString("NOTORG.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.FileReader"));

            testee.apply(source, dest, null);
            testee.apply(dest, source, null);

            final Optional<ModuleDependency> moduleDependency1 = moduleGraph.findDependency(OTHER, IO);
            final Optional<ModuleDependency> moduleDependency2 = moduleGraph.findDependency(IO, OTHER);

            assertThat(moduleDependency1.isPresent()).isTrue();
            assertThat(moduleDependency2.isPresent()).isTrue();
        }

        @Test
        @DisplayName("should not add self dependencies")
        public void testNoSelfDep() {
            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.core.FileReader"));

            testee.apply(source, dest, null);

            final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(CORE, CORE);

            assertThat(moduleDependency.isPresent()).isFalse();
        }

        @Test
        @DisplayName("should not add self dependency to other")
        public void testNoSelfToOther() {
            final AccessPoint source = AccessPoint.create(ElementName.fromString("NOTORG.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("NOTORG.example.core.FileReader"));

            testee.apply(source, dest, null);

            final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(OTHER, OTHER);

            assertThat(moduleDependency.isPresent()).isFalse();
        }

        @Test
        @DisplayName("should add source and dest to more modules if multiple regexes match")
        public void testMultiAdd() {
            final List<HWModule> repeatedModules = Arrays.asList(CORE, SUPER_MODULE, IO);
            final MutableNetwork<HWModule, ModuleDependency> graph = NetworkBuilder.directed().build();
            final GuavaModuleGraph moduleGraph = new GuavaModuleGraph(graph);
            final ModuleDependenciesGraphBuildingVisitor testee = new ModuleDependenciesGraphBuildingVisitor<>(
                    repeatedModules, moduleGraph, OTHER, builder);

            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Component"));

            testee.apply(source, dest, null);

            final Optional<ModuleDependency> moduleDependency1 = moduleGraph.findDependency(CORE, SUPER_MODULE);
            final Optional<ModuleDependency> moduleDependency2 = moduleGraph.findDependency(CORE, IO);
            final Optional<ModuleDependency> moduleDependency3 = moduleGraph.findDependency(SUPER_MODULE, IO);

            assertThat(moduleDependency1.isPresent()).isTrue();
            assertThat(moduleDependency2.isPresent()).isTrue();
            assertThat(moduleDependency3.isPresent()).isTrue();
        }

        @Test
        @DisplayName("should warn if more module match")
        public void testWarnMultiAdd() {
            final List<HWModule> repeatedModules = Arrays.asList(CORE, SUPER_MODULE, IO);
            final MutableNetwork<HWModule, ModuleDependency> graph = NetworkBuilder.directed().build();
            final GuavaModuleGraph moduleGraph = new GuavaModuleGraph(graph);
            final ModuleDependenciesGraphBuildingVisitor testee = new ModuleDependenciesGraphBuildingVisitor<>(
                    repeatedModules, moduleGraph, OTHER, builder, warningsCollector);

            final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Service"));
            final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.Component"));

            testee.apply(source, dest, null);

            assertThat(visitWarningsContainPairMatching(source.getElementName(), CORE, SUPER_MODULE)).isTrue();
            assertThat(visitWarningsContainPairMatching(dest.getElementName(), IO, SUPER_MODULE)).isTrue();
        }

        private boolean visitWarningsContainPairMatching(ElementName ap, HWModule... modules) {
            boolean match = false;
            for (int i = 0; !match && i < visitWarnings.size(); ++i) {
                final Pair<ElementName, Collection<HWModule>> pair = visitWarnings.get(i);
                final List<HWModule> expected = Arrays.asList(modules);
                match = pair.first.equals(ap) && pair.second.containsAll(expected) && expected.containsAll(pair.second);
            }
            return match;
        }
    }
}
