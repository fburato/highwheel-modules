# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

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
