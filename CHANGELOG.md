# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
