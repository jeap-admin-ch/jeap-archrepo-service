# Getting started

The arch repo service is a **library**. It is embedded into a jEAP microservice, which supplies the runtime
configuration - the database, the credentials of the systems to import from, and the schedules. There is no
`spring-boot-maven-plugin` and no `application.yml` in this repository, and the artifact is not deployable on its
own.

## Prerequisites

- A jEAP microservice, i.e. a project whose parent chain reaches `ch.admin.bit.jeap:jeap-spring-boot-parent`
- A PostgreSQL database for the architecture model
- An OAuth2 authorization server, for the endpoints protected with semantic roles - see [Security](security.md)

## Add the dependency

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-archrepo-instance</artifactId>
    <version>${jeap-archrepo.version}</version>
</dependency>
```

`jeap-archrepo-instance` is a `pom`-packaged aggregator pulling in `jeap-archrepo-web` - which carries the
`@SpringBootApplication` and all logic - together with `jeap-archrepo-test`. Depend on `jeap-archrepo-web` alone
if the instance does not want the test module.

## Minimal configuration

```yaml
spring:
  application:
    name: my-archrepo-service
  datasource:
    url: jdbc:postgresql://localhost:5432/archrepo
    username: archrepo
    password: ${DB_PASSWORD}

server:
  servlet:
    context-path: /${spring.application.name}

archrepo-config:
  environment: dev

archrepo:
  openapi-base-url: "https://archrepo.example.com/my-archrepo-service/swagger-ui/index.html?url=/my-archrepo-service/api/openapi/"
  update-schedule: "0 35 1 * * *"
  api:
    secret: "{noop}${ARCHREPO_API_SECRET}"
  documentation-generator:
    update-schedule: "0 50 2 * * *"
    confluence:
      mock-confluence-client: true

jeap:
  security:
    oauth2:
      resourceserver:
        system-name: "my-system"
```

That starts a service with **no importer active**: every importer is registered conditionally on its own
configuration, so an instance opts in to the sources it has. See [Importers](importers.md) for what to add, and
[Configuration](configuration.md) for the complete property list.

`jeap.security.oauth2.resourceserver.system-name` is what enables **semantic authorization**, and it is
**required**: the [docs API](docs-api.md) authorizes every one of its resources with a semantic role, so the
service does not start without it - see [Security](security.md).

## Database

The Flyway migrations ship with `jeap-archrepo-persistence` under `db/migration/common` and run while the service
starts. The database user therefore needs permission to create tables in its schema.

## Verifying the instance

1. The service starts and the actuator reports it healthy.
2. `GET /api/model` answers with the systems - an empty list on a fresh database.
3. Create the first system. No importer does this, and an importer skips every component whose system it cannot
   find, so an import into an empty model imports nothing:

   ```http
   POST /api/management/system
   Content-Type: application/json

   { "name": "foo", "teamName": "team-blue" }
   ```

4. Trigger an import without waiting for the schedule:

   ```http
   POST /api/jobs
   Content-Type: application/json

   { "type": "UPDATE_MODEL" }
   ```

   See [API](api.md) for the job endpoints and [Operations](operations.md) for the rest of the manual
   maintenance.

## Related

- [Concepts](concepts.md) - what the arch repo is for, and the conventions an instance has to follow
- [Architecture](architecture.md) - what the library is built from
- [Importers](importers.md) - the sources the model is built from
- [Operations](operations.md) - the jobs, monitoring, and creating the first system
- [Configuration](configuration.md) - every property
- [Security](security.md) - authentication and the semantic roles
- [API](api.md) - the REST API
