# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.27.1] - 2025-07-11

### Changed

- Fixed the rendering of nullable columns in the database schemas diagrams

## [1.27.0] - 2025-07-11

### Changed

- Render documentation for uploaded database schemas

## [1.26.0] - 2025-07-10

### Changed

- OpenAPI/DB Schema Upload: Automatically create the associated system component and system if they don’t already exist

## [1.25.0] - 2025-07-08

### Changed

- Update parent from 26.67.0 to 26.68.0

## [1.24.0] - 2025-07-04

### Changed

- Update parent from 26.64.2 to 26.67.0
- update org.eclipse.jgit from 7.1.0.202411261347-r to 7.3.0.202506031305-r
- update wiremock-standalone from com.github.tomakehurst:wiremock-jre8-standalone 2.35.2 to org.wiremock:wiremock-standalone 3.13.1
- update commons-io from 2.18.0 to 2.19.0
- update guava from 33.3.1-jre to 33.4.8-jre

## [1.23.0] - 2025-07-03

- Remove 'system' Attribute in CreateOrUpdateDatabaseSchema

## [1.22.1] - 2025-06-26

### Bugfixes

- Use sonar-maven-plugin 5.1.0.4751 to fix SonarQube analysis

## [1.22.0] - 2025-06-25

### Changed

- Added support for associating system components with database schemas

## [1.21.0] - 2025-06-24

### Changed

- Added support for reactions containing 2+ actions
- Improved rendering of reaction statistics
- Update parent from 26.63.0 to 26.64.2

## [1.20.0] - 2025-06-19

### Changed

- Update parent from 26.61.0 to 26.63.0

## [1.19.0] - 2025-06-17

### Changed

- Update parent from 26.59.0 to 26.61.0

## [1.18.0] - 2025-06-16

### Changed

- Update parent from 26.57.0 to 26.59.0

## [1.17.0] - 2025-06-13

### Changed

- Update parent from 26.55.0 to 26.57.0

## [1.16.1] - 2025-06-10

### Changed

- Fixing a bug that avoided model update after a parent upgrade

## [1.16.0] - 2025-06-06

### Changed

- Update parent from 26.54.0 to 26.55.0

## [1.15.1] - 2025-06-04

### Changed

- Manually setting commons-beanutils to 1.11.0 as previous versions contain a vulnerability

## [1.15.0] - 2025-06-03

### Changed

- Added support for statistics from jEAP Reaction Observer Service
- Update parent from 26.43.2 to 26.54.0

## [1.14.0] - 2025-04-15

### Changed

- Update parent from 26.43.1 to 26.43.2

## [1.13.0] - 2025-04-09

### Changed

- Update parent from 26.42.0 to 26.43.1
- Remove usage of com.google.common.eventbus.EventBus

## [1.12.0] - 2025-04-01

### Changed

- Update parent from 26.33.0 to 26.42.0

## [1.11.0] - 2025-03-06

### Changed

- Update parent from 26.24.2 to 26.33.0

## [1.10.0] - 2025-02-18

### Changed

- Prepare repository for Open Source distribution

## [1.9.0] - 2025-02-13

### Changed

- Update parent from 26.23.0 to 26.24.2
- Disable license plugins for service instances

## [1.8.0] - 2025-02-10

### Changed

- Update parent from 26.22.0 to 26.23.0

## [1.7.1] - 2025-01-14

### Changed

- Added jeap-archrepo-test as test dependency to jeap-archrepo-instance as this dependency should be used by
  archrepo instances to implement pact tests. 

## [1.7.0] - 2025-01-13

### Changed

- Added the module jeap-archrepo-instance which will instantiate an architecture repository instance when used as parent project.

## [1.6.0] - 2025-01-06

### Changed

- OpenAPI Spec Upload: parse file content to retrieve defined paths

## [1.5.1] - 2025-01-06

### Added
- Added a new Pact provider state to support Pact tests with the governance dashboard as a new Pact consumer.
- Upgraded to latest jeap parent 26.22.0.

## [1.5.0] - 2024-12-20

### Added

- Added PACT test base classes to be executed by archrepo instances.

## [1.4.0] - 2024-12-19

### Changed

- Update parent from 26.17.0 to 26.21.1

## [1.3.1] - 2024-12-16

### Added
- The possibility to have no PactBroker or no Message contract configuration.

## [1.3.0] - 2024-12-13

### Added
- Added an endpoint for the retrieval of the API documentation versions of all system components.

## [1.2.1] - 2024-12-12

### Fixed
- configure dependency to rhos importer in web pom and service pom

### Added
- The possibility to have no CloudFoundry or no RHOS configuration, AWS is already conditional

## [1.2.0] - 2024-12-11

### Added
- The possibility to import metrics from RHOS Grafana

## [1.1.0] - 2024-11-28

### Changed
- Configure default properties for the application

## [1.0.0] - 2024-11-28

### Added
- Initial revision imported from applicationplatform-archrepo-service
