package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.core.externaladapters.JungModuleGraph;
import com.github.fburato.highwheelmodules.core.model.HWModule;
import com.github.fburato.highwheelmodules.core.model.ModuleDependency;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.junit.Test;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

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
    return new Pair<T1, T2>(first, second);
  }

  private final List<HWModule> constructionWarnings = new ArrayList<HWModule>(5);
  private final List<Pair<ElementName, Collection<HWModule>>> visitWarnings =
      new ArrayList<Pair<ElementName, Collection<HWModule>>>(5);

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

  private final DirectedSparseGraph<HWModule, ModuleDependency> graph = new DirectedSparseGraph<HWModule, ModuleDependency>();
  private final JungModuleGraph moduleGraph = new JungModuleGraph(graph);
  private final WarningsCollector warningsCollector = new AddToListWarnings();
  private final ModuleDependenciesGraphBuildingVisitor.DependencyBuilder<ModuleDependency> builder = (m1, m2, source, dest, type) -> new ModuleDependency(m1, m2);
  private final ModuleDependenciesGraphBuildingVisitor<ModuleDependency> testee =
      new ModuleDependenciesGraphBuildingVisitor<>(modules, moduleGraph, OTHER, builder);

  @Test
  public void constructorShouldAddAllModulesToTheModuleGraph() {
    final List<HWModule> allModules = new ArrayList<>(modules.size() + 1);
    allModules.addAll(modules);
    allModules.add(OTHER);
    assertThat(graph.getVertices().containsAll(allModules)).isTrue();
    assertThat(allModules.containsAll(graph.getVertices())).isTrue();
  }

  @Test
  public void constructorShouldRemarkRepeatedModules() {
    final List<HWModule> repeatedModules = Arrays.asList(
        HWModule.make("Core", "org.example.core.*").get(),
        HWModule.make("Core", "org.example.io.*").get()
    );
    final DirectedSparseGraph<HWModule, ModuleDependency> graph = new DirectedSparseGraph<HWModule, ModuleDependency>();
    final JungModuleGraph moduleGraph = new JungModuleGraph(graph);
    final WarningsCollector warningsCollector = new AddToListWarnings();
    new ModuleDependenciesGraphBuildingVisitor<>(repeatedModules, moduleGraph, OTHER, builder, warningsCollector);

    assertThat(constructionWarnings.size()).isEqualTo(1);
    assertThat(constructionWarnings.get(0).name).isEqualTo("Core");
  }

  @Test
  public void applyShouldAddSourceAndDestToTheAppropriateModules() {
    final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Service"));
    final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.FileReader"));

    testee.apply(source, dest, null);

    final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(CORE, IO);

    assertThat(moduleDependency.isPresent()).isTrue();
  }

  @Test
  public void applyShouldNotConnectionInUnmatchingElements() {
    final AccessPoint source = AccessPoint.create(ElementName.fromString("NOTORG.example.core.Service"));
    final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.io.FileReader"));

    testee.apply(source, dest, null);

    final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(CORE, IO);

    assertThat(moduleDependency.isPresent()).isFalse();
  }

  @Test
  public void applyShouldConnectUnmatchingElementsToOther() {
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
  public void applyShouldNotAddSelfDependencies() {
    final AccessPoint source = AccessPoint.create(ElementName.fromString("org.example.core.Service"));
    final AccessPoint dest = AccessPoint.create(ElementName.fromString("org.example.core.FileReader"));

    testee.apply(source, dest, null);

    final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(CORE, CORE);

    assertThat(moduleDependency.isPresent()).isFalse();
  }

  @Test
  public void applyShouldNotAddSelfDependencyToOther() {
    final AccessPoint source = AccessPoint.create(ElementName.fromString("NOTORG.example.core.Service"));
    final AccessPoint dest = AccessPoint.create(ElementName.fromString("NOTORG.example.core.FileReader"));

    testee.apply(source, dest, null);

    final Optional<ModuleDependency> moduleDependency = moduleGraph.findDependency(OTHER, OTHER);

    assertThat(moduleDependency.isPresent()).isFalse();
  }

  @Test
  public void applyShouldAddSourceAndDestToMoreModulesIfMoreModuleGlobRegexMatch() {
    final List<HWModule> repeatedModules = Arrays.asList(
        CORE,
        SUPER_MODULE,
        IO
    );
    final DirectedSparseGraph<HWModule, ModuleDependency> graph = new DirectedSparseGraph<HWModule, ModuleDependency>();
    final JungModuleGraph moduleGraph = new JungModuleGraph(graph);
    final ModuleDependenciesGraphBuildingVisitor testee =
        new ModuleDependenciesGraphBuildingVisitor<>(repeatedModules, moduleGraph, OTHER, builder);

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
  public void applyShouldAddWarningsIfMoreModuleGlobRegexMatch() {
    final List<HWModule> repeatedModules = Arrays.asList(
        CORE,
        SUPER_MODULE,
        IO
    );
    final DirectedSparseGraph<HWModule, ModuleDependency> graph = new DirectedSparseGraph<HWModule, ModuleDependency>();
    final JungModuleGraph moduleGraph = new JungModuleGraph(graph);
    final ModuleDependenciesGraphBuildingVisitor testee =
        new ModuleDependenciesGraphBuildingVisitor<>(repeatedModules, moduleGraph, OTHER, builder, warningsCollector);

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
