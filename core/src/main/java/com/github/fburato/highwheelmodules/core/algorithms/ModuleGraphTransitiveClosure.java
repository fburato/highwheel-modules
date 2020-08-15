package com.github.fburato.highwheelmodules.core.algorithms;

import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.modules.ModuleDependency;
import com.github.fburato.highwheelmodules.model.modules.ModuleGraph;

import java.util.*;

/**
 * Calculate the Transitive closure of a ModuleGraph using the Floyd-Warshall algorithm Ref Cormen, Thomas H.;
 * Leiserson, Charles E.; Rivest, Ronald L. (1990). Introduction to Algorithms (1st ed.). MIT Press and McGraw-Hill.
 * ISBN 0-262-03141-8. See in particular Section 26.2, "The Floyd–Warshall algorithm" pp. 558–565
 */
public class ModuleGraphTransitiveClosure {

    public static class Difference {
        public final HWModule source;
        public final HWModule dest;
        public final int firstDistance;
        public final int secondDistance;

        public Difference(HWModule source, HWModule dest, int firstDistance, int secondDistance) {
            this.source = source;
            this.dest = dest;
            this.firstDistance = firstDistance;
            this.secondDistance = secondDistance;
        }

        @Override
        public String toString() {
            return "Difference{" + "source=" + source + ", dest=" + dest + ", firstDistance=" + firstDistance
                    + ", secondDistance=" + secondDistance + '}';
        }
    }

    public static class PathDifference {
        public final HWModule source;
        public final HWModule dest;
        public final List<HWModule> firstPath;
        public final List<HWModule> secondPath;

        public PathDifference(HWModule source, HWModule dest, List<HWModule> firstPath, List<HWModule> secondPath) {
            this.source = source;
            this.dest = dest;
            this.firstPath = firstPath;
            this.secondPath = secondPath;
        }

        @Override
        public String toString() {
            return "PathDifference{" + "source=" + source + ", dest=" + dest + ", firstPath=" + firstPath
                    + ", secondPath=" + secondPath + '}';
        }
    }

    private final List<HWModule>[][] minimumPathMatrix;
    private final Map<HWModule, Integer> indexMap;
    private final Collection<HWModule> modules;

    public ModuleGraphTransitiveClosure(ModuleGraph<ModuleDependency> moduleGraph, Collection<HWModule> modules) {
        this.modules = modules;
        minimumPathMatrix = initialiseSquareMatrixTo(modules.size());
        indexMap = createMapModuleIndex(modules);

        initialiseDistanceOneModules(modules, moduleGraph);

        applyFloydWarshallMainIteration();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<HWModule>[][] initialiseSquareMatrixTo(final int size) {
        final List<HWModule>[][] array = (List<HWModule>[][]) new List[size][size];
        for (int i = 0; i < array.length; ++i)
            for (int j = 0; j < array.length; ++j)
                array[i][j] = new ArrayList<>();
        return array;
    }

    private Map<HWModule, Integer> createMapModuleIndex(Collection<HWModule> modules) {
        final Map<HWModule, Integer> map = new HashMap<>(modules.size());
        int moduleCount = 0;
        for (HWModule module : modules) {
            map.put(module, moduleCount++);
        }
        return map;
    }

    private void initialiseDistanceOneModules(Collection<HWModule> modules, ModuleGraph<ModuleDependency> moduleGraph) {
        for (HWModule module : modules) {
            for (HWModule dependency : moduleGraph.dependencies(module)) {
                minimumPathMatrix[indexMap.get(module)][indexMap.get(dependency)].add(dependency);
            }
        }
    }

    private void applyFloydWarshallMainIteration() {
        for (int i = 0; i < minimumPathMatrix.length; ++i) {
            for (int j = 0; j < minimumPathMatrix.length; ++j) {
                for (int k = 0; k < minimumPathMatrix.length; ++k) {
                    List<HWModule> pathIJ = minimumPathMatrix[i][j];
                    List<HWModule> pathIK = minimumPathMatrix[i][k];
                    List<HWModule> pathKJ = minimumPathMatrix[k][j];
                    if (pathIJ.isEmpty() && !pathIK.isEmpty() && !pathKJ.isEmpty()) {
                        minimumPathMatrix[i][j].clear();
                        minimumPathMatrix[i][j].addAll(pathIK);
                        minimumPathMatrix[i][j].addAll(pathKJ);
                    } else if (!pathIJ.isEmpty() && !pathIK.isEmpty() && !pathKJ.isEmpty()) {
                        if (pathIK.size() + pathKJ.size() < pathIJ.size()) {
                            minimumPathMatrix[i][j].clear();
                            minimumPathMatrix[i][j].addAll(pathIK);
                            minimumPathMatrix[i][j].addAll(pathKJ);
                        }
                    }
                }
            }
        }
    }

    public Boolean isReachable(HWModule vertex1, HWModule vertex2) {
        return minimumDistance(vertex1, vertex2).map((a) -> a < Integer.MAX_VALUE).orElse(false);
    }

    public boolean same(ModuleGraphTransitiveClosure other) {
        return diff(other).map(List::isEmpty).orElse(false);
    }

    public Optional<List<Difference>> diff(ModuleGraphTransitiveClosure other) {
        return diffPath(other).map((argument) -> {
            final List<Difference> result = new ArrayList<>(argument.size());
            for (PathDifference pathDifference : argument) {
                result.add(new Difference(pathDifference.source, pathDifference.dest, pathDifference.firstPath.size(),
                        pathDifference.secondPath.size()));
            }
            return result;
        });
    }

    public Optional<List<PathDifference>> diffPath(ModuleGraphTransitiveClosure other) {
        if (!modules.containsAll(other.modules) || !other.modules.containsAll(modules))
            return Optional.empty();
        final List<PathDifference> differences = new ArrayList<>();
        for (HWModule i : modules) {
            for (HWModule j : modules) {
                int thisI = indexMap.get(i), thisJ = indexMap.get(j), otherI = indexMap.get(i),
                        otherJ = indexMap.get(j);
                if (minimumPathMatrix[thisI][thisJ].size() != other.minimumPathMatrix[otherI][otherJ].size())
                    differences.add(new PathDifference(i, j, minimumPathMatrix[thisI][thisJ],
                            other.minimumPathMatrix[otherI][otherJ]));
            }
        }
        return Optional.of(differences);
    }

    public Optional<Integer> minimumDistance(HWModule vertex1, HWModule vertex2) {
        if (indexMap.get(vertex1) == null || indexMap.get(vertex2) == null)
            return Optional.empty();
        else {
            final int distance = minimumPathMatrix[indexMap.get(vertex1)][indexMap.get(vertex2)].size();
            return Optional.of(distance == 0 ? Integer.MAX_VALUE : distance);
        }
    }

    public List<HWModule> minimumDistancePath(HWModule vertex1, HWModule vertex2) {
        if (indexMap.get(vertex1) == null || indexMap.get(vertex2) == null)
            return new ArrayList<>();
        else
            return minimumPathMatrix[indexMap.get(vertex1)][indexMap.get(vertex2)];
    }
}
