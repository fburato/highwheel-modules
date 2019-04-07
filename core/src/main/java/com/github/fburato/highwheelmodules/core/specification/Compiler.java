package com.github.fburato.highwheelmodules.core.specification;

import com.github.fburato.highwheelmodules.model.modules.AnonymousModule;
import com.github.fburato.highwheelmodules.model.modules.Definition;
import com.github.fburato.highwheelmodules.model.modules.HWModule;
import com.github.fburato.highwheelmodules.model.rules.Dependency;
import com.github.fburato.highwheelmodules.model.rules.NoStrictDependency;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.fburato.highwheelmodules.utils.StringUtil.join;

public class Compiler {

    public static final String MODULE_REGEX_NOT_WELL_DEFINED = "Regular expression '%s' of module '%s' is not well defined";
    public static final String MODULE_ALREADY_DEFINED = "HWModule '%s' has already been defined";
    public static final String MODULE_HAS_NOT_BEEN_DEFINED = "HWModule '%s' referenced in rule '%s' has not been defined";

    private static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    public Definition compile(SyntaxTree.Definition definition) {
        final String prefix = definition.prefix.orElse("");
        final Map<String, HWModule> modules = compileModules(definition.moduleDefinitions, prefix);
        final Pair<List<Dependency>, List<NoStrictDependency>> rules = compileRules(definition.rules, modules);
        final Optional<AnonymousModule> whitelist = compileRegexesOrFail(definition.whiteList,
                "Some of the whitelist regular expressions are malformed");
        final Optional<AnonymousModule> blacklist = compileRegexesOrFail(definition.blackList,
                "Some of the blacklist regular expressions are malformed");

        return new Definition(whitelist, blacklist, modules.values(), rules.first, rules.second);
    }

    private Optional<AnonymousModule> compileRegexesOrFail(Optional<List<String>> blackList, String failureMessage) {
        return blackList
                .map(regexes -> AnonymousModule.make(regexes).orElseThrow(() -> new CompilerException(failureMessage)));
    }

    private Map<String, HWModule> compileModules(List<SyntaxTree.ModuleDefinition> definitions, String prefix) {
        final Map<String, HWModule> modules = new HashMap<>(definitions.size());

        for (SyntaxTree.ModuleDefinition moduleDefinition : definitions) {
            final Optional<HWModule> optionalModule = HWModule.make(moduleDefinition.moduleName,
                    moduleDefinition.moduleRegex.stream().map(regex -> prefix + regex).collect(Collectors.toList()));
            if (!optionalModule.isPresent()) {
                throw new CompilerException(String.format(MODULE_REGEX_NOT_WELL_DEFINED, moduleDefinition.moduleRegex,
                        moduleDefinition.moduleName));
            } else if (modules.get(moduleDefinition.moduleName) != null) {
                throw new CompilerException(String.format(MODULE_ALREADY_DEFINED, moduleDefinition.moduleName));
            } else {
                modules.put(moduleDefinition.moduleName, optionalModule.get());
            }
        }

        return modules;
    }

    private Pair<List<Dependency>, List<NoStrictDependency>> compileRules(List<SyntaxTree.Rule> rulesDefinition,
            Map<String, HWModule> modules) {
        final List<Dependency> dependencies = new ArrayList<>();
        final List<NoStrictDependency> noDirectDependencies = new ArrayList<>();
        for (SyntaxTree.Rule ruleDefinition : rulesDefinition) {
            if (ruleDefinition instanceof SyntaxTree.ChainDependencyRule) {
                final SyntaxTree.ChainDependencyRule chainDependencyRule = (SyntaxTree.ChainDependencyRule) ruleDefinition;
                dependencies.addAll(compileChainDependencies(chainDependencyRule.moduleNameChain, modules));
            } else if (ruleDefinition instanceof SyntaxTree.NoDependentRule) {
                final SyntaxTree.NoDependentRule noDependentRule = (SyntaxTree.NoDependentRule) ruleDefinition;
                noDirectDependencies.add(compileNoDependency(noDependentRule, modules));
            } else if (ruleDefinition instanceof SyntaxTree.OneToManyRule) {
                final SyntaxTree.OneToManyRule rule = (SyntaxTree.OneToManyRule) ruleDefinition;
                dependencies.addAll(compileOneToMany(rule, modules));
            } else if (ruleDefinition instanceof SyntaxTree.ManyToOneRule) {
                final SyntaxTree.ManyToOneRule rule = (SyntaxTree.ManyToOneRule) ruleDefinition;
                dependencies.addAll(compileManyToOne(rule, modules));
            }
        }
        return new Pair<>(dependencies, noDirectDependencies);
    }

    private List<Dependency> compileChainDependencies(List<String> chainDependencies, Map<String, HWModule> modules) {
        final List<Dependency> result = new ArrayList<>(chainDependencies.size() - 1);
        for (int i = 0; i < chainDependencies.size() - 1; ++i) {
            final String current = chainDependencies.get(i);
            final String next = chainDependencies.get(i + 1);
            if (modules.get(current) == null) {
                throw new CompilerException(
                        String.format(MODULE_HAS_NOT_BEEN_DEFINED, current, join(" -> ", chainDependencies)));
            } else if (modules.get(next) == null) {
                throw new CompilerException(
                        String.format(MODULE_HAS_NOT_BEEN_DEFINED, next, join(" -> ", chainDependencies)));
            } else {
                result.add(new Dependency(modules.get(current), modules.get(next)));
            }
        }
        return result;
    }

    private List<Dependency> compileOneToMany(SyntaxTree.OneToManyRule oneToManyRule, Map<String, HWModule> modules) {
        final Function<String, CompilerException> makeCompilerException = module -> new CompilerException(
                String.format(MODULE_HAS_NOT_BEEN_DEFINED, module,
                        String.format("%s -> (%s)", oneToManyRule.one, join(", ", oneToManyRule.many))));
        if (modules.get(oneToManyRule.one) == null) {
            throw makeCompilerException.apply(oneToManyRule.one);
        }
        return oneToManyRule.many.stream().map(dest -> {
            if (modules.get(dest) == null) {
                throw makeCompilerException.apply(dest);
            } else {
                return new Dependency(modules.get(oneToManyRule.one), modules.get(dest));
            }
        }).collect(Collectors.toList());
    }

    private List<Dependency> compileManyToOne(SyntaxTree.ManyToOneRule manyToOneRule, Map<String, HWModule> modules) {
        final Function<String, CompilerException> makeCompilerException = module -> new CompilerException(
                String.format(MODULE_HAS_NOT_BEEN_DEFINED, module,
                        String.format("(%s) -> %s", join(", ", manyToOneRule.many), manyToOneRule.one)));
        if (modules.get(manyToOneRule.one) == null) {
            throw makeCompilerException.apply(manyToOneRule.one);
        }
        return manyToOneRule.many.stream().map(dest -> {
            if (modules.get(dest) == null) {
                throw makeCompilerException.apply(dest);
            } else {
                return new Dependency(modules.get(dest), modules.get(manyToOneRule.one));
            }
        }).collect(Collectors.toList());
    }

    private NoStrictDependency compileNoDependency(SyntaxTree.NoDependentRule noDependentRule,
            Map<String, HWModule> modules) {
        if (modules.get(noDependentRule.left) == null) {
            throw new CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, noDependentRule.left,
                    join(" -/-> ", Arrays.asList(noDependentRule.left, noDependentRule.right))));
        } else if (modules.get(noDependentRule.right) == null) {
            throw new CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, noDependentRule.right,
                    join(" -/-> ", Arrays.asList(noDependentRule.left, noDependentRule.right))));
        } else {
            return new NoStrictDependency(modules.get(noDependentRule.left), modules.get(noDependentRule.right));
        }
    }
}
