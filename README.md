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
Modules ::= "modules:" "\n"
              { ModuleDefinition }
            "rules:" "\n"
              { RuleDefinition } 

ModuleDefinition ::= ModuleIdentifier = ModuleRegex{ , ModuleRegex } "\n"

ModuleIdentifier ::= <java identifier>

ModuleRegex ::= "<glob regex>"

RuleDefinition ::= DependencyRule | NoDependencyRule

DependencyRule ::= <java identifier> "->" <java identifier> { "->" <java identifier> } "\n"

NoDependencyRule ::= <java identifier> "-/->" <java identifier>
```

In order for a specification to be compiled correctly:

* At least one module needs to be defined.
* All the identifiers used in the rules section need to be defined in the modules section.
* The file needs to end with a new-line

## Modes of operation

Highwheel modules supports two modes of operation: **strict** and **loose**.

When running on strict mode, the rules are interpreted as follows:

* `A -> B` requires that there must exist a direct dependency between a class in module `A` and a class 
in module `B`. The rule is violated if there is no such dependency or if `A` depends on `B` indirectly through other
modules
* `A -/-> B` requires that if there `B` is reachable from `A` then there is no explicit dependency between classes
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


## Command line tool

The highwheel modules command line tool is contained in the module `highwheel-modules-cli`. In order to
build the tool just build the entire project with 

```
mvn clean install
```

The executable jar will be available at `highwheel-modules-cli/target/modulesAnalyser-jar-with-dependencies.jar`.

### Using the command line tool

The command line tool base syntax is

```
java -jar moduleAnalyser-jar-with-dependencies.jar <directories and jar to analyse>
```

The tool will:

* Read a module specification file named `spec.hwm` in the current working directory and compile it, reporting any 
errors.
* Read all `*.class` files in the directories and jars passed as argument
* Start a strict analysis using the specification read from `spec.hwm` on the resources passed.
* Provide the result of the analysis and the fan-in/fan-out measurement on all modules.

It is possible to change both the specification file and the mode with the following options:

* `--spec | -s`: path to the specification file to use.
* `--mode | -m (strict | loose)`: run strict or loose analysis.

## Highwheel modules maven plugin

The highwheel modules maven plugin is contained in the module `highwheel-modules-maven`. In order to build the tool,
just build the entire project with:

```
mvn clean install
```

### Using the plugin

In order to run the plugin on a project execute:

```
mvn com.github.fburato:highwheel-modules-maven:analyse
```

The plugin will:

* Read a module specification file named `spec.hwm` in the project base-directory and compile it, reporting any 
errors.
* Read all `*.class` files in the project output directory (`target/classes`)
* Start a strict analysis using the specification read from `spec.hwm` on the resources passed.
* Provide the result of the analysis and the fan-in/fan-out measurement on all modules.
* The build will fail if any of the rules are violated

It is possible to change the behaviour of the plugin as follows:

* `-DspecFile=<path to spec file>`: use the path provided instead of `spec.hwm` from the base dir if the path is 
relative, otherwise use the path as is if the path is absolute.
* `-DchildOnly=true`: in a multi-module build, run the analysis only on the child modules.
* `-DparentOnly=true`: in a multi-module build, run the analysis only on the parent.
* `-DanalysisMode=(strict|loose)`: run strict or loose analysis

In a multi-module build, the plugin will use all the child output directories as elements of the analysis.