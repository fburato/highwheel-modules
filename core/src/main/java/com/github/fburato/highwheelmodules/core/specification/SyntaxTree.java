package com.github.fburato.highwheelmodules.core.specification;

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
        public final List<ModuleDefinition> moduleDefinitions;
        public final List<Rule> rules;

        public Definition(List<ModuleDefinition> moduleDefinitions, List<Rule> rules) {
            this(Optional.empty(), Optional.empty(), Optional.empty(), moduleDefinitions, rules);
        }

        public Definition(Optional<String> prefix, List<ModuleDefinition> moduleDefinitions, List<Rule> rules) {
            this(prefix, Optional.empty(), Optional.empty(), moduleDefinitions, rules);
        }

        public Definition(Optional<String> prefix, Optional<List<String>> whiteList, Optional<List<String>> blackList,
                List<ModuleDefinition> moduleDefinitions, List<Rule> rules) {
            this.prefix = prefix;
            this.whiteList = whiteList;
            this.blackList = blackList;
            this.moduleDefinitions = moduleDefinitions;
            this.rules = rules;
        }

        @Override
        public String toString() {
            return "Definition{" + "prefix=" + prefix + ", whiteList=" + whiteList + ", blackList=" + blackList
                    + ", moduleDefinitions=" + moduleDefinitions + ", rules=" + rules + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Definition that = (Definition) o;
            return Objects.equals(prefix, that.prefix) && Objects.equals(whiteList, that.whiteList)
                    && Objects.equals(blackList, that.blackList)
                    && Objects.equals(moduleDefinitions, that.moduleDefinitions) && Objects.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, whiteList, blackList, moduleDefinitions, rules);
        }
    }
}
