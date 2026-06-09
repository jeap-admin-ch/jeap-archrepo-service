# AGENTS.md

This file provides guidance to codign agents when working with code in this repository.

## What this is

`jeap-archrepo-service` is a **jEAP Spring Boot library** (not a standalone deployable). It is embedded as a Maven
dependency into a concrete jEAP microservice that supplies the runtime config (DB, Confluence credentials,
Prometheus/service hosts). There is no `spring-boot-maven-plugin`/repackage and no `application.yml` in this repo —
`jeap-archrepo-web` ships the `@SpringBootApplication` (`ArchRepoApplication`) and all logic; `jeap-archrepo-instance`
is a `pom`-packaged aggregator that pulls `web` + `test` together for a deployment.

Its job: continuously import the architecture inventory of a jEAP landscape from multiple data sources into a unified
model in PostgreSQL, then auto-generate Confluence documentation from that model.

## Build & test

Always use the Maven wrapper `./mvnw` (Java 25, Spring Boot 4).

```bash
./mvnw clean install                          # full reactor build + all tests
./mvnw -q -pl jeap-archrepo-docgen -am test   # build one module (+ its deps) and test it
./mvnw -pl jeap-archrepo-docgen test -Dtest=ConfluenceAdapterImplTest          # single test class
./mvnw -pl jeap-archrepo-docgen test -Dtest=ConfluenceAdapterImplTest#deleteOrphanPages  # single method
```

Note: a full `clean install` regenerates `THIRD-PARTY-LICENSES.md` via the license plugin, but in a local/offline run it
produces an **incomplete** file (drops most deps). Do not commit that regeneration — revert it (
`git checkout -- THIRD-PARTY-LICENSES.md`); the file is maintained by the CI/release pipeline.

## Architecture

### Data flow

```
external sources → ArchRepoImporter plugins → ArchitectureModel → JPA entities → PostgreSQL → DocumentationGenerator → Confluence
```

Two scheduled jobs in `jeap-archrepo-web/.../web/service/UpdateService.java`, each guarded by ShedLock (
`@SchedulerLock`, so only one instance runs in a cluster):

- `updateModel()` — cron `${archrepo.update-schedule}`: loads the model, runs every importer, saves the model.
- `generateDocumentation()` — cron `${archrepo.documentation-generator.update-schedule}`: loads the model and renders it
  to Confluence.

### Importer plugin pattern (the core extension point)

Importers implement `ArchRepoImporter` (`jeap-archrepo-importers`):

```java
int getOrder();                                                  // lower runs first

void importIntoModel(ArchitectureModel model, String environment);
```

`UpdateService` injects `List<ArchRepoImporter>`, sorts by `getOrder()`, and runs each. Each importer lives in its own
`jeap-archrepo-importer-*` module with an `@AutoConfiguration` that registers the bean **conditionally** (
`@ConditionalOnProperty`, or the custom `@ConditionalOnNonEmptyHosts` for the Prometheus variants) — if its config is
absent, the importer simply isn't active. There is no hard-coded importer list.

`getOrder()` encodes source precedence: metrics importers (prometheus-aws/rhos) run before `deploymentlog` so
live-metrics data wins over deployment-log data when both describe the same component. Entities carry an `Importer` enum
marking their source, which lets a scheduled importer remove elements that have disappeared from its source.

Not all ingestion is scheduled: OpenAPI specs and database schemas are pushed in synchronously via REST controllers in
`jeap-archrepo-web/.../web/rest/` (those importers are plain `@Component`s, not `ArchRepoImporter`s).

### The model: transient aggregate over persistent entities

`ArchitectureModel` (`jeap-archrepo-metamodel`) is a plain in-memory object (builder, **no `@Entity`**) holding
`List<System>` + `List<Team>`. Its contents *are* JPA entities, though: `System` is the aggregate root (`@OneToMany`
cascade/orphan-removal over `SystemComponent`, `Event`, `Command`, relation subtypes, `RestApi`, `OpenApiSpec`, database
schemas). `ArchitectureModelRepository` in `jeap-archrepo-persistence` bridges the two — `load()` assembles the
transient model from the JPA repositories, `save()` persists the graph back. Flyway migrations live in
`jeap-archrepo-persistence/src/main/resources/db/migration/common`.

### Documentation generation → Confluence

`jeap-archrepo-docgen`: `DocumentationGenerator` walks the model (system page → component/event/command sub-pages),
renders HTML with Thymeleaf templates (`src/main/resources/template/documentation/`), and generates graph PNG
attachments. All Confluence I/O goes through the `ConfluenceAdapter` interface:

- `ConfluenceAdapterImpl` — real Confluence via the `asciidoc-confluence-publisher-client` library.
- `ConfluenceAdapterMock` — selected when `archrepo.documentation-generator.confluence.mock-confluence-client=true` (
  used by all tests).

Confluence client specifics (changed recently — see CHANGELOG 6.2.0): the publisher client splits into v1 (Server/Data
Center, `ConfluenceRestV1Client`, `/rest/api`) and v2 (Cloud) variants. This project uses **v1**. On v1,
`getPageByTitle(spaceKey, ancestorId, title)` filters by ancestor, so the root page can't be found by title alone — it
is configured by **ID** via `archrepo.documentation-generator.confluence.root-page-id` (not by name).

### Configuration prefixes

- `archrepo-config` → `ArchRepoConfigProperties` (the `environment` enum, passed to every importer to filter
  stage-specific data).
- `archrepo.update-schedule`, `archrepo.documentation-generator.update-schedule` → the two scheduler crons.
- `archrepo.documentation-generator.confluence.*` → `DocumentationGeneratorConfluenceProperties` (`url`, `space-key`,
  `root-page-id`, credentials, `mock-confluence-client`). Bound with `ignoreUnknownFields = false`, so a stale/renamed
  key here **fails startup** rather than being ignored.
- Per-importer prefixes (e.g. `messages.message-contract-service-uri`, the prometheus host lists) gate importer
  activation.

## Testing patterns

JUnit 5 + Mockito throughout. External HTTP is faked with WireMock. **Pact** is central: consumer contract tests for the
upstream jEAP services this app calls (deploymentlog, message-contract, reaction-observer) and provider verification,
with shared base classes and stubs in the `jeap-archrepo-test` module. Confluence is never hit in tests — they run with
`mock-confluence-client: true`, so the real `ConfluenceRestV1Client` path is unverified by the suite and needs a staging
smoke test before release.

## Versioning & Conventions

- Semantic Versioning; all changes documented in [CHANGELOG.md](./CHANGELOG.md) (Keep a Changelog format).
- `setPomVersions.sh` updates the version across all module POMs.
- When working on a feature branch, increase the version to `x.y.z-SNAPSHOT` in the POMs.
- When bumping the version, also update the changelog, and updates version/date in `publiccode.yml`.
- When the version on a feature branch has not yet been bumped compared to master, ask the user if a major, minor or
  patch version bump should be performed, and update the version accordingly.
