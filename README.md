# jEAP ArchRepo - Library
 
## Purpose
The `jeap-archrepo-service` is a jEAP library designed to be integrated into applications through simple dependency configuration. 
This library provides a centralized system for automatically managing and documenting the application architecture inventory.

## Integration

### Preconditions
* The microservice must be a jEAP Microservice.
* The service needs a database (PostgreSQL) so you have to create/order one

### Maven Dependency
Add the following dependency to your `pom.xml`:
    
    ```xml
        <dependency>
            <groupId>ch.admin.bit.jeap</groupId>
            <artifactId>jeap-archrepo-service</artifactId>
            <version>[version]</version>
        </dependency>
    ```

## Changes
This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Note

This repository is part the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
