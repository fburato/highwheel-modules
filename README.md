[![Build Status](https://github.com/fburato/highwheel-modules/actions/workflows/scala.yml/badge.svg)](https://github.com/fburato/highwheel-modules/actions/workflows/scala.yml)
# Highwheel-Modules

Highwheel modules is an extension of the class dependency visualisation tool [Highwheel](https://github.com/hcoles/highwheel)
by Henry Coles to express, measure and verify the structure and the design of Java projects.

In general it is reasonable to expect that Java project are organised in logical entities (or modules) that serve a 
specific concern: a module is used to contain the core business logic of the application, a module is used to 
accomodate the outer interface through a web api, a module is used to contain the clients to connect to persistent 
storage devices and external services etc. Highwheel modules offers:

* A language to describe and define software modules and the relation between them (module specification).
* An analysis scanner that takes the classes of a project and fits them in the defined modules.
* A dependency calculator that determines if the provided module specification is observed by the project or not.
* Command line tool and a maven plugin to apply validate the specification.
* Measurement of architectural metrics (fan-in and fan-out) useful to verify stability and abstractness of the modules.

## Specification language

Highwheel-module specification language can be described by the following grammar in EBNF form:

```
Modules ::= ["prefix:" RegexLiteral "\n"]
            ["whitelist:" RegexLiteral{, RegexLiteral} "\n"]
            ["blacklist:" RegexLiteral{, RegexLiteral} "\n"]
            ["mode:" Mode]
            "modules:" "\n"
              { ModuleDefinition }
            "rules:" "\n"
              { RuleDefinition } 

ModuleDefinition ::= ModuleIdentifier = RegexLiteral{ , RegexLiteral } "\n"

ModuleIdentifier ::= <java identifier>

RegexLiteral ::= "<glob regex>"

Mode ::= "STRICT" | "LOOSE"

RuleDefinition ::= DependencyRule | NoDependencyRule | OneToManyRule | ManyToOneRule

DependencyRule ::= <java identifier> "->" <java identifier> { "->" <java identifier> } "\n"

NoDependencyRule ::= <java identifier> "-/->" <java identifier>

OneToManyRule ::= <java identifier> "->" "(" <java identifier> {"," <java identifier>} ")"

ManyToOneRule ::= "(" <java identifier> {"," <java identifier>} ")" "->" <java identifier>
```

In order for a specification to be compiled correctly:

* At least one module needs to be defined.
* All the identifiers used in the rules section need to be defined in the modules section.
* The file needs to end with a new-line

An example of specification can be found in this project in the `spec.hwm` files of every project modules and would look
like this:

```
prefix: "com.github.fburato.highwheelmodules."

modules:
    Utils = "utils.*"
    Core = "core.*"
    Cli = "cli.*"
    MavenPlugin = "maven.*"
    Parser = "bytecodeparser.*"
    Model = "model.*"
rules:
    (MavenPlugin, Cli) -> Core
    Core -> Parser
    (Core, MavenPlugin, Cli, Parser, Model) -> Utils
    (Core, Parser) -> Model
```

An equivalent way of providing the specification is to use the `prefix` preamble, which allows to automatically
add to all module specification a prefix to compact the definition.

With the usage of prefix, the following definition:

```
modules:
    Algorithms = "com.github.fburato.highwheelmodules.core.algorithms.*"
    ExternalAdapters = "com.github.fburato.highwheelmodules.core.externaladapters.*"
    Specification = "com.github.fburato.highwheelmodules.core.specification.*"
    ModuleAnalyser = "com.github.fburato.highwheelmodules.core.analysis.*"
    Facade = "com.github.fburato.highwheelmodules.core.AnalyserFacade"

rules:
    Facade -> (ModuleAnalyser, Specification, ExternalAdapters)
    ModuleAnalyser -> Algorithms
    Facade -/-> Algorithms
```

is equivalent to

```
prefix: "com.github.fburato.highwheelmodules.core."

modules:
    Algorithms = "algorithms.*"
    ExternalAdapters = "externaladapters.*"
    Specification = "specification.*"
    ModuleAnalyser = "analysis.*"
    Facade = "AnalyserFacade"

rules:
    Facade -> (ModuleAnalyser, Specification, ExternalAdapters)
    ModuleAnalyser -> Algorithms
    Facade -/-> Algorithms
```

Whitelisting and blacklisting of modules is also supported (as of `1.5.0`). By specifying whitelist and blacklists
in the specification, you can force Highwheel modules to focus only on certain classes or exclude certain classes from 
analysis respectively. 

By whitelisting you are forcing the bytecode analyser to consider elements identified to be added to the dependency 
graph building algorithm only if they match any of the regexes in the whitelist.

By blacklisting, you are forcing the bytecode analyser to ignore elements identified to be added to the dependency
graph building algorithm if they match any of the regexes in the blacklist.
## Modes of operation

Highwheel modules supports two modes of operation: **strict** and **loose**.

When running on strict mode, the rules are interpreted as follows:

* `A -> B` requires that there must exist a direct dependency between a class in module `A` and a class 
in module `B`. The rule is violated if there is no such dependency or if `A` depends on `B` indirectly through other
modules
* `A -/-> B` requires that if `B` is reachable from `A` then there is no explicit dependency between classes
of `A` and classes of `B`. The rule is violated if there is such a direct dependency.

Moreover, a strict analysis fails if there are dependencies in the actual dependency graph calculated from the bytecode
that do not appear in the specification. Basically a strict analysis requires the entire dependency graph to be
explicitly written in the specification in order for it to pass.

It is an analysis mode suggested to identify circular dependencies (i.e. if there is no such rule as `A -> A` in the
specification but the circular dependency on `A` exists, the analysis will fill) and to enforce strong design decisions.

When running on loose mode, the rules are interpreted as follows:
                           
* `A -> B` requires that `B` is reachable from `A` in any way. The rule is violated if `B` is not reachable from `A`
* `A -/-> B` requires that `B` is not reachable from `A` in any way. The rule is violated if `A` depends on `B`

Basically, the loose analysis is a whitelisting and blacklisting analysis: certain dependency are allowed to exist
and certain are not.

It is an analysis mode suggested to ensure very specific properties in the dependency graph and not the entire
structure of it.

In order to use the loose analysis mode, specify the mode in the specification file as follows.


```
prefix: "com.github.fburato.highwheelmodules.core."
mode: LOOSE

modules:
    Algorithms = "algorithms.*"
    ExternalAdapters = "externaladapters.*"
    Specification = "specification.*"
    ModuleAnalyser = "analysis.*"
    Facade = "AnalyserFacade"

rules:
    Facade -> (ModuleAnalyser, Specification, ExternalAdapters)
    ModuleAnalyser -> Algorithms
    Facade -/-> Algorithms
```

The default mode is `STRICT`, but the mode can be explicitly indicated with `mode: STRICT` in the same position.

## Usage 

Highwheel modules can be used by including in your build the appropriate plugin, depending on your build tool of choice:

- Maven: include in your build the [highwheel-modules-maven-plugin](https://github.com/fburato/highwheel-modules-maven-plugin).
- SBT: include in your build the [sbt-highwheel](https://github.com/fburato/sbt-highwheel) plugin.
