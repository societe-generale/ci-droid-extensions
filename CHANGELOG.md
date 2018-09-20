# Changelog - see https://keepachangelog.com for conventions

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

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


