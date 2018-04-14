# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

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