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
| `archrepo.api.secret`         | yes      | -       | The password of the technical user for HTTP basic on `/api/**` - see [Security](security.md). Use a password-encoder prefix, e.g. `{noop}` for a plain value |
| `archrepo.api.username`       | no       | `archrepo-admin` | The username of the technical user for HTTP basic on `/api/**` - see [Security](security.md)                                                              |

## Systems and teams: config as master

Declare the complete landscape under `archrepo.model` in the instance's deployment repository:

```yaml
archrepo:
  model:
    teams:
      - name: team-blue
        contact-address: team-blue@example.ch
        confluence-link: https://example.ch/wiki/blue
        jira-link: https://example.ch/jira/BLUE
    systems:
      - name: foo
        team: team-blue
        description: The foo system
        confluence-link: https://example.ch/wiki/foo
        aliases: [fo]
```

Both lists must be non-empty. If either is empty or missing, onboarding logs a WARN and makes **no changes**:
no creation, update or deletion. Once active, this is a full declaration: missing optional fields are cleared,
aliases are replaced, and systems/teams not declared are deleted. An explicit empty string clears a string field;
`aliases: []` or an omitted `aliases` clears all aliases. Every system must reference a declared team;
a team present only in the database does not satisfy that reference.
Names and aliases share a case-insensitive namespace; team names are also unique ignoring case. Duplicate names,
conflicting aliases and undeclared teams fail startup, as do unknown keys, blank required names/references/aliases
and descriptions longer than 1024 characters - a broken document never reaches a model update. (An inactive
document, with either list empty, is not cross-checked.) Other fields have no length limit.
Names/aliases of a system deleted in the same update are freed; a name or alias still held by a retained system
cannot be taken over by another entry in that update. These checks need the stored model, so they fail the update
instead, before any change.

Existing database names and aliases are checked for case-insensitive ambiguity too. If legacy rows have duplicate
system/team names or conflicting aliases, onboarding fails before changing the model rather than choosing a row.
The error identifies the conflicting names. Export preserves these rows verbatim; resolve the ambiguity before
mounting the document, even if an ambiguous entry would otherwise be deleted.

A case-only name edit preserves identity and normalizes spelling. A different name deletes the old system and
creates the new one. To carry the old system's components over, list the old name (or one of its old aliases) as
an alias of the new system - or of another retained system - in the same change: the components then move there
with their REST APIs, OpenAPI specs and database schemas, keeping their identity. The old system's events, commands
and the relations it defines are deleted with it, and come back with the next run of their importers. Without such
an alias, everything of the old system is deleted.

Moving an alias between two retained systems takes two deployments, because a retained system's alias cannot be
taken over in the same update: remove it from the first, then add it to the second. Importers then move the
components that resolve through it, again with their REST APIs, OpenAPI specs and database schemas.

Changing a system's team
does not rewrite existing component owners; if their old team is removed, they fall back to the system's default owner.
New components inherit the system's default owner at creation. List order is irrelevant.

For the rationale behind these rules, see [Onboarding design decisions](architecture.md#onboarding-design-decisions).

### Bootstrap and GitOps rollout

1. Deploy 18.0.0 without `archrepo.model`; existing model data is untouched.
2. Export immediately before rollout and review the complete document for case-insensitive duplicate names/aliases:
   `curl --fail -H 'Accept: application/yaml' https://archrepo.example.ch/api/model/export > model.yml`.
3. Commit it to the instance's deployment repository and mount it, for example at `/etc/archrepo/model.yml`.
   Set `SPRING_CONFIG_IMPORT=file:/etc/archrepo/model.yml`.
4. **Redeploy/restart**, then trigger `POST /api/jobs` with `{"type":"UPDATE_MODEL"}` or wait for the schedule.
   The export reads the primary database, not a replica, so it includes recently written metadata.

Configuration binds only at startup. Changing a ConfigMap without restarting has no effect. Every landscape instance
must use the same document; roll it out atomically because the instance holding ShedLock applies its own copy.
Subsequent changes follow commit → deploy/restart → next model update, including single-importer updates.
For drift detection, periodically export and diff against the committed, deterministically ordered YAML.
The export is unauthenticated and includes team contacts and links.

**Deletion warning:** removing a system deletes its aggregate, including cross-system relations, pushed OpenAPI specs,
database schemas and component descriptions. Importers rebuild observed data, but specs/schemas need another push and
component descriptions require a backup. Take a database backup before the first authoritative run and large changes.
An active document cannot intentionally reduce the model to zero systems: that requires manual database maintenance.

## Docs API

See [Docs API](docs-api.md).

The docs API is always active; it has no property of its own.

| Property                                          | Required | Default | Description                                                                                                                          |
| ------------------------------------------------- | -------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `jeap.security.oauth2.resourceserver.system-name` | yes      | -       | Activates semantic authorization, without which the docs API's role cannot be evaluated. **The service refuses to start without it** |

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
