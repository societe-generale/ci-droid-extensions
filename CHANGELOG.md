# Changelog - see https://keepachangelog.com for conventions

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

## [1.0.10] - 2022-04-05

### Changed
- upgraded libraries : logback-classic, commons-io, dom4j, junit
- replaced Travis CI by Github actions for build activities 

## [1.0.9] - 2019-03-13

### Fixed 
- in AddXmlContentAction, elementToAdd is now a textArea, and xpathUnderWhichElementNeedsToBeAdded is a textField (was declared vice-versa)


## [1.0.8] - 2019-01-16

### Added
- adding DeleteResourceAction, as required by https://github.com/societe-generale/ci-droid-tasks-consumer/issues/50

## [1.0.7] - 2019-01-11

### Changed
- elementToAdd in AddXmlContentAction is now a textArea
- upgrading to ci-droid internal-api 1.0.6
- making ci-droid internal-api a "provided" dependencies, to avoid version conflicts in projects that will use both

## [1.0.6] - 2018-11-08

### Changed
- upgrading to ci-droid internal-api 1.0.5

### Fixed
- Issue #9 : TemplateBasedContentAction now inits itself properly
- Issue #11 : trimming elementsToAdd to avoid parsing errors

## [1.0.5] - 2018-09-20

### Changed
- Issue #3 : refactored ReplaceMavenProfileAction
- refactored XML actions, introduced AbstractXmlProcessingAction 
- Issue #6 : renamed AddXmlElementAction into AddXmlContentAction - not limited to single element anymore

## [1.0.4] - 2018-08-14

### Added
- new RemoveMavenDependencyOrPluginAction
- some tests, to show examples

### Fixed
- issues related to namespace in AddXmlElementAction 
- documented how to specify the xpath regardless of namespace when using RemoveXmlElementAction

## [1.0.3] - 2018-08-10

### Fixed
- reviewed Lombok annotations : classes without the NoArgConstructor can't be deserialized by Jackson

## [1.0.2] - 2018-08-04

### Changed
- upgraded to internal-api 1.0.2

## [1.0.1] - 2018-07-11

### Added
- adding AddXmlElementAction
- adding RemoveXmlElementAction

### Changed
- now it can be released via Travis

## [1.0.0] - 2018-06-19

first version !


