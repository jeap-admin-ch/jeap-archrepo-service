# Configuration

Every property below is supplied by the **instance** - the microservice that embeds the library. There is no
`application.yml` in this repository.

Most `@ConfigurationProperties` classes bind with `ignoreUnknownFields = false`, so a stale or misspelled key
**fails the startup** instead of being silently ignored.

## Core

| Property                      | Required | Default | Description                                                                                                                                                |
| ----------------------------- | -------- | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `archrepo-config.environment` | yes      | -       | The stage this instance describes: `DEV`, `REF`, `ABN` or `PROD`. Passed to every importer so environment-specific sources are queried for the right stage |
| `archrepo.update-schedule`    | yes      | -       | Cron expression for the model update job. `-` disables it                                                                                                  |
| `archrepo.openapi-base-url`   | yes      | -       | Prefix used to build the Swagger UI link of a component; the service **fails to start** when it is missing. `{system}/{component}` is appended, lowercased |
| `archrepo.api.secret`         | yes      | -       | The password of the `api` user for HTTP basic on `/api/**` - see [Security](security.md). Use a password-encoder prefix, e.g. `{noop}` for a plain value   |

## Docs API

See [Docs API](docs-api.md).

The docs API is always active; it has no property of its own.

| Property                                          | Required | Default | Description                                                                                                                          |
| ------------------------------------------------- | -------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `jeap.security.oauth2.resourceserver.system-name` | yes      | -       | Activates semantic authorization, without which the docs API's role cannot be evaluated. **The service refuses to start without it** |

## Documentation generator

See [Documentation generator](documentation-generator.md).

| Property                                                             | Required | Default | Description                                                     |
| -------------------------------------------------------------------- | -------- | ------- | --------------------------------------------------------------- |
| `archrepo.documentation-generator.update-schedule`                   | yes      | -       | Cron expression for the Confluence export. `-` disables it      |
| `archrepo.documentation-generator.confluence.url`                    | yes      | -       | Base URL of the Confluence instance                             |
| `archrepo.documentation-generator.confluence.space-key`              | yes      | -       | The space the documentation is written to                       |
| `archrepo.documentation-generator.confluence.root-page-id`           | yes      | -       | **Id**, not name, of the page the generated tree hangs under    |
| `archrepo.documentation-generator.confluence.username`               | yes      | -       | Technical user                                                  |
| `archrepo.documentation-generator.confluence.password`               | yes      | -       | Its password                                                    |
| `archrepo.documentation-generator.confluence.mock-confluence-client` | no       | `false` | When `true` nothing is written to Confluence. Used by all tests |

## Importers

Each importer activates itself when its configuration is present - see [Importers](importers.md).

### Deployment log

| Property                                           | Description                                                             |
| -------------------------------------------------- | ----------------------------------------------------------------------- |
| `deploymentlog.url`                                | Base URL of the jEAP deployment log service. **Activates the importer** |
| `deploymentlog.username`, `deploymentlog.password` | Credentials                                                             |

### Metrics on AWS

| Property                                                      | Default | Description                                     |
| ------------------------------------------------------------- | ------- | ----------------------------------------------- |
| `prometheus-aws.host`                                         | -       | Prometheus endpoint. **Activates the importer** |
| `prometheus-aws.workspace`                                    | -       | Workspace id                                    |
| `prometheus-aws.role-arn`, `prometheus-aws.role-session-name` | -       | The role assumed to read the workspace          |
| `prometheus-aws.timeout`                                      | `120s`  | Request timeout                                 |
| `prometheus-aws.max-in-memory-size`                           | `16MB`  | Response buffer limit                           |

### Metrics on RHOS

| Property                  | Default | Description                                                                               |
| ------------------------- | ------- | ----------------------------------------------------------------------------------------- |
| `prometheus-rhos.hosts`   | -       | List of `host` / `service-account-token` pairs. **Activates the importer** when non-empty |
| `prometheus-rhos.timeout` | `120s`  | Request timeout                                                                           |

### Message types

| Property                                | Description                                                        |
| --------------------------------------- | ------------------------------------------------------------------ |
| `messages.message-contract-service-uri` | The message contract service. **Activates the importer**           |
| `messages.git-uris`                     | Message type registry repositories to read the descriptors from    |
| `messages.repositories`                 | The same, in the extended form with `uri`, `type` and `parameters` |

### Pact broker

| Property                                     | Description                                             |
| -------------------------------------------- | ------------------------------------------------------- |
| `pactbroker.url`                             | Base URL of the Pact broker. **Activates the importer** |
| `pactbroker.username`, `pactbroker.password` | Credentials                                             |

### Reaction observer

| Property                                                               | Description                                                           |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `reactionobserverservice.url`                                          | Base URL of the reaction observer service. **Activates the importer** |
| `reactionobserverservice.username`, `reactionobserverservice.password` | Credentials                                                           |

## Security

`jeap.security.oauth2.resourceserver.system-name` enables **semantic authorization** and is what the semantic
roles are resolved against. It is **required**: the [docs API](docs-api.md) authorizes every one of its resources
with a semantic role, so the service does not start without it - see [Security](security.md).

The remaining security properties come from the jEAP security starter.

## Database

PostgreSQL with Flyway; the migrations ship in `jeap-archrepo-persistence` under `db/migration/common` and run at
startup. The instance configures the connection and how it is authenticated.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/archrepo
    username: archrepo
    password: ${DB_PASSWORD}
```

## Related

- [Getting started](getting-started.md) - a minimal instance configuration
- [Importers](importers.md) - what each importer contributes
- [Operations](operations.md) - what the schedules actually do, and what else the runtime needs
- [Security](security.md) - authentication and roles
