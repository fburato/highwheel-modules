# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [1.6.0] - Unreleased
Version `1.6.0` introduce a breaking change on the analyser behaviour. Specifically, it removes the 
possibility to provide the analysis mode externally. The reason for this change is because an hwm specification
is typically designed to be run with a specific mode as it is rare that the same specification is valid
for both strict and loose. Given this strong relationship, it makes sense to have the mode of execution
introduced in the specification itself. In order to support this feature, a new `mode` section is introduced
in the specification language.

Moreover, since spring is over, spring cleaning was overdue, and the JUnit 5 test facilities have been
extended to most of the tests and a few more cleanup have been added.

### Changed

- **Remove the execution mode from AnalyserFacade**


### Added

- **`mode` section in the grammar** 


## [1.5.0] - 2019-04-07
`1.5.0` is the biggest release of HWM so far. More importantly, the release is completely backward compatible,
but huge improvements have been made to the specification grammar! The more important changes are:

- **New rules syntax**. While using HWM I've realised that the specification can become very verbose for very specific
modules. For example, if you have the "end of the world" main module that just performs the necessary application 
wiring, that module will depend on every other module. Until `1.4.0` you could add one dependency per line and you could
chain dependencies together, but with `1.5.0` new rules allow to quickly create many dependencies in one line in the
form `(A,B,C) -> D` and `A -> (B,C,D)`.
- **Whitelisting and blacklisting**. Another limitation I've noticed while using the plugins is that although the 
multiple analysis at once feature is incredibly cool (if I may say so `:P`), it is limited by the fact that since
the classpath is shared across the all analysis, then you have to introduce top-level packages to sub-modules, which makes
the specification weird. The solution to this problem I've come up with is "whitelisting" and "blacklisting". Basically,
every specification includes a list of regexes that can be used to limit the analyser to focus only on certain
packages or to ignore specific packages. Multi-spec analysis becomes significantly more ergonomic and the specifications
can be made "fractal" (i.e. you can create many spec file for the same codebase and define the architectural specs
for internal pacakges). Word of caution: **introducing whitelisting has some risks**. One of the most powerful features
of HWM is the detection of transitive dependencies through modules that are not explicitly part of the module specification.
By whitelisting only certain packages you are losing this safety-net, so, be cautious.
- **Support for Java 12 bytecode**. The new ASM library has been added, hence all bytecode until Java 12 can be parsed.
- **Dependency from highwheel removed**. I've included in the project the modules that I was importing from highwheels by
copying over the source code and the tests. Given the new release train for Java I want to get more control over the
release of the necessary components, and considering the dependency on ASM is the main reason why I included highwheels
in the first place and Henry is not particularly interested in maintaining the library, I just moved the code here.
I'll always maintain the reference to the original project in the documentation, but the time has come to HWM to be
more independent. This resulted in the limitation of the dependencies to `jparsec` and `guava`, which is great.

### Changed
- **Changed graph library implementation**. By experimenting with the introduction of new the `module-info`s, I've noticed
that the Jung graph library has what is called `split-package` problem, i.e. it has two artifacts that define classes
in the same package. This doesn't work well with Jigsaw's module system because it is required for every package to be
exported and provided by one module and one module only. To solve the issue, I've changed the library for graph processing
to Guava, which has the non-trivial advantage of being actively developed.

## [1.4.0] - 2018-10-03

Aren't you tired of having to specify the complete package name for all module regex even if they all begin with the
same kilometric prefix? Me too! Now the specification file can contain a `prefix` preamble that automatically adds
a given prefix to all regex in the module specification. Saving precious bytes in the disk and making everything more
readable.

## [1.3.0] - 2018-10-01

Highwheel-modules used to be terrible at multi-tasking: you could perform multiple analysis using the same 
classpath and different specifications, but the scanner had to parse the bytecode in the classpath for every single one 
of them. Thanks to a complex incantation consisting in a few clever ideas (limit evidence collection and associated
definitions to dependency graphs) and some hat-tricks, now highwheel-modules can accept any number of specification,
compile them, run all the analysis while scanning the classpath and reporting all results at the end. The 
analysis could even be parallelised, but as Spock once said: premature optimisation evil is Harry, now on the 
Millenium Falcon embark you must.

### Changed

- The Analyser Facade now accepts a list of files for specification and an explicit evidence limit (optional).
An empty evidence limit will leave the analyser to collect all evidence (high memory footprint).
- The `hwmSpecFile` option in the maven plugin has been renamed to `hwmSpecFiles` and its value needs to be a comma
separated list of paths.
- The command line interface now accepts multiple `-s` option to pass multiple specification files.


## [1.2.0] - 2018-04-16

Sometimes less is more, sometimes more is less. Sometimes 1000 pieces of evidence that show that yes, indeed module B depends module A are a little too much considering one piece of evidence is sufficient to disprove a universal quantifier.

Because of this, it's probably better to leave the choice to the user and show just 5 pieces of evidence by default with the configuration in the maven plugin `hwmEvidenceLimit`.

### Added
- `hwmEvidenceLimit` in the maven plugin configuration allows to limit the number of evidence shown in case of violation. Putting it to 0 will silence the evidences entirely. Mom's the word. Sshhhhhh.

## [1.1.1] - 2018-04-15
Semantics change on the evidence provider: instead of following a path, it returns all access points that make hwm 
determine that two modules are dependent. This should provide a better alignment between the module output and the 
evidences

### Changed
- The interface for the events in AnalyserFacade has been changed to host all recorded dependencies that violate the
specification

### Deleted
- The EvidenceFinder class. Probably the thing that took me the longest to develop to produce 1.1.0 is gone. It will be
missed (*not by me*).

## [1.1.0] - 2018-04-15
Focus of the release is increasing the visibility of dependency violations. As a project grows, the module 
specification can grow too making the regexp used to describe the modules in the specification ginormous. The end result
could be that HWM is telling you "Hey, there's a dependency you where not aware of between A -> B" but doesn't tell you 
where exactly which is not very helpful if there are hundreds of classes in a module. With 1.1.0, I've added an 
additional tracking mechanism that keeps track of every access point connection and provides back that information.

### Changed

- The module dependency AccessVisitor has been made more generic to allow adding functionalities without rewriting the 
core logic
- The interface for the events in AnalyserFacade has been changed to include the access path evidence that violates the 
module dependency. 


## [1.0.1] - 2018-04-13

There was a typo on some artifact names (`highweel` instead of `highwheel`) that I didn't caught and that was hurting
my eyes. That is no more. The interface, the name of  packages and everything else remains unchanged.

### Changed

- `highweel-modules-parent`, `highweel-modules-utils`, `highweel-modules-cli` and `highweel-modules-maven-plugin` 
artifacts renamed to `highweel-modules-parent`, `highwheel-modules-utils`, `highwheel-modules-cli` and 
`highwheel-modules-maven-plugin`.

## [1.0.0] - 2018-04-13

First version of the standalone project. The original release has been made in 
[my fork of Henry's highwheel](https://github.com/fburato/highwheel) project but after discussing the matter with Henry,
we decided to split them. This is due to various reasons:

- Highwheel aim is class visualisation. Highwheel-Modules focuses on static analysis. As a Cthulhu High Priest and a 
separation of concerns maniac, this seemed wrong to me. So, here we are :)
- I want to keep updating Highwheel-Modules and release versions independently of the direction of Highwheel. 
Considering I don't want to burden Henry more than absolutely necessary with pull requests and such, I'd rather take 
100% responsibility on the maintenance of the project.
- I'd really like this project to be adopted to the wider Java/Scala/Kotlin community because I think it can be useful 
to fill execution the gap between software architecture and software development. Having a dedicated project with its 
own documentation and a laser focus on its main purpose I believe will be beneficial to the adoption.
