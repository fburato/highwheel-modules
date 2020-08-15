package com.github.fburato.highwheelmodules.core.specification;

import com.github.fburato.highwheelmodules.utils.Builder;

import java.util.*;

public interface SyntaxTree {

    final class ModuleDefinition {
        public final String moduleName;
        public final List<String> moduleRegex;

        public ModuleDefinition(String moduleName, String moduleRegex) {
            this.moduleName = moduleName;
            this.moduleRegex = Collections.singletonList(moduleRegex);
        }

        public ModuleDefinition(String moduleName, List<String> moduleRegexs) {
            this.moduleName = moduleName;
            this.moduleRegex = moduleRegexs;
        }

        @Override
        public String toString() {
            return "ModuleDefinition{" + "moduleName='" + moduleName + '\'' + ", stringLiteral='" + moduleRegex + '\''
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ModuleDefinition that = (ModuleDefinition) o;

            return Objects.equals(this.moduleName, that.moduleName)
                    && Objects.equals(this.moduleRegex, that.moduleRegex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, moduleRegex);
        }
    }

    interface Rule {
    }

    final class ChainDependencyRule implements Rule {
        public final List<String> moduleNameChain;

        public ChainDependencyRule(List<String> moduleNameChain) {
            this.moduleNameChain = moduleNameChain;
        }

        public ChainDependencyRule(String... moduleNameChain) {
            this(Arrays.asList(moduleNameChain));
        }

        @Override
        public String toString() {
            return "ChainDependencyRule{" + "moduleNameChain=" + moduleNameChain + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ChainDependencyRule that = (ChainDependencyRule) o;

            return moduleNameChain.equals(that.moduleNameChain);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleNameChain);
        }
    }

    final class NoDependentRule implements Rule {
        public final String left;
        public final String right;

        public NoDependentRule(String left, String right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "NoDependentRule{" + "left='" + left + '\'' + ", right='" + right + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            NoDependentRule that = (NoDependentRule) o;

            return Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }
    }

    final class OneToManyRule implements Rule {
        public final String one;
        public final List<String> many;

        public OneToManyRule(String one, List<String> many) {
            this.one = one;
            this.many = many;
        }

        @Override
        public String toString() {
            return "OneToManyRule{" + "one='" + one + '\'' + ", many=" + many + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            OneToManyRule that = (OneToManyRule) o;
            return Objects.equals(one, that.one) && Objects.equals(many, that.many);
        }

        @Override
        public int hashCode() {
            return Objects.hash(one, many);
        }
    }

    final class ManyToOneRule implements Rule {
        public final List<String> many;
        public final String one;

        public ManyToOneRule(List<String> many, String one) {
            this.many = many;
            this.one = one;
        }

        @Override
        public String toString() {
            return "ManyToOneRule{" + "many=" + many + ", one='" + one + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ManyToOneRule that = (ManyToOneRule) o;
            return Objects.equals(many, that.many) && Objects.equals(one, that.one);
        }

        @Override
        public int hashCode() {
            return Objects.hash(many, one);
        }
    }

    class Definition {
        public final Optional<String> prefix;
        public final Optional<List<String>> whiteList;
        public final Optional<List<String>> blackList;
        public final Optional<String> mode;
        public final List<ModuleDefinition> moduleDefinitions;
        public final List<Rule> rules;

        public Definition(Optional<String> prefix, Optional<List<String>> whiteList, Optional<List<String>> blackList,
                Optional<String> mode, List<ModuleDefinition> moduleDefinitions, List<Rule> rules) {
            this.prefix = prefix;
            this.whiteList = whiteList;
            this.blackList = blackList;
            this.mode = mode;
            this.moduleDefinitions = moduleDefinitions;
            this.rules = rules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Definition that = (Definition) o;
            return Objects.equals(prefix, that.prefix) && Objects.equals(whiteList, that.whiteList)
                    && Objects.equals(blackList, that.blackList) && Objects.equals(mode, that.mode)
                    && Objects.equals(moduleDefinitions, that.moduleDefinitions) && Objects.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, whiteList, blackList, mode, moduleDefinitions, rules);
        }

        @Override
        public String toString() {
            return "Definition{" + "prefix=" + prefix + ", whiteList=" + whiteList + ", blackList=" + blackList
                    + ", mode=" + mode + ", moduleDefinitions=" + moduleDefinitions + ", rules=" + rules + '}';
        }

        public static class DefinitionBuilder extends Builder<Definition, SyntaxTree.Definition.DefinitionBuilder> {

            public Optional<String> prefix;
            public Optional<List<String>> whiteList;
            public Optional<List<String>> blackList;
            public Optional<String> mode;
            public List<SyntaxTree.ModuleDefinition> moduleDefinitions;
            public List<SyntaxTree.Rule> rules;

            private DefinitionBuilder() {
                super(DefinitionBuilder::new);
            }

            @Override
            protected SyntaxTree.Definition makeValue() {
                return new SyntaxTree.Definition(prefix, whiteList, blackList, mode, moduleDefinitions, rules);
            }

            public static DefinitionBuilder baseBuilder() {
                return new DefinitionBuilder().with($ -> {
                    $.prefix = Optional.empty();
                    $.whiteList = Optional.empty();
                    $.blackList = Optional.empty();
                    $.mode = Optional.empty();
                    $.moduleDefinitions = new ArrayList<>();
                    $.rules = new ArrayList<>();
                });
            }
        }
    }
}
