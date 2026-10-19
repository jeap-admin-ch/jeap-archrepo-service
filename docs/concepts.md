# Concepts

The arch repo service is an inventory of the architecture a landscape **actually has**, not of the architecture
someone once designed. Its guiding principle is that the inventory is kept current **automatically wherever
possible**: everything that another system already knows is imported from there, and only what nothing can derive
is maintained by hand.

## The problem it solves

A landscape of a few dozen jEAP microservices knows all of its own facts, but no single place holds them:

- The deployment log knows what was rolled out, but not who owns it.
- Prometheus knows what is actually running, but only for as long as it runs.
- The message type registry knows every event and command, but not who publishes or consumes it.
- The Pact broker knows which service calls which REST endpoint, but only where a contract test exists.
- Every service knows its own OpenAPI spec and its own database schema, and nobody else does.

Hand-maintained architecture documentation goes stale the moment any of that changes. The arch repo instead reads
all of these sources on a schedule, merges them into one model, and keeps that model - and the documentation
generated from it - in step with reality.

## Scope

The arch repo describes **system architecture**, in black-box terms:

- which systems exist, and which team owns each of them
- which components belong to which system, and of what kind they are
- which events and commands the systems define, and which components have a contract for them
- which REST APIs the components provide, and who calls them
- which OpenAPI spec and which database schema each component published

Deliberately **not** in scope:

- the white-box view of a single service - its internal design belongs to that service's own documentation
- business or enterprise architecture
- the *target* architecture; the model only ever describes what is there now

## The conventions the automation relies on

Because components are discovered rather than declared, the importers derive structure from naming. Two
conventions matter:

**A component name starts with the name of its system.** The deployment log importer and the AWS metrics importer
split a component name at the first `-` and look the leading segment up as a system, so `foo-bar-service` is filed
under the system `foo`. The RHOS metrics importer gets the system from the namespace instead, which it expects to
look like `something-foo-stage`. The Pact broker importer expects consumer and provider names to read
`department-system-component` and takes the middle segment.

A system whose names do not follow the convention can declare **aliases**; a lookup matches the system name or
any of its aliases, ignoring case.

**The component type is derived from the suffix.** `SystemComponentFactory` creates a `Frontend` for a name ending
in `-ui`, `-frontend` or `-webui`, a `Gateway` for one ending in `-gateway`, a `SelfContainedSystem` for one
ending in `-scs`, and a `BackendService` for everything else. `MobileApp` and `UnknownSystemComponent` exist in the model but are not produced by an importer.

## Systems are not discovered - components are

An importer only ever files a component into a system **that already exists**. Both the deployment log importer and
the metrics importers log a warning and skip a component whose system they cannot find, and neither creates one.

So onboarding a system is a deliberate act: create it - with its name, its owning team and any aliases - through
`POST /api/management/system`, and the importers fill in its components from then on. See
[Operations](operations.md).

The one exception is a database schema push: `SystemComponentService.findOrCreateSystemComponent()` creates the
component it names, and if the system derived from its prefix does not exist either, it creates that too - with a
team of the same name as its default owner. That is the only path on which a system appears without anyone asking
for it. See [API](api.md).

## What consumes the model

| Consumer                   | How                                                                                                                                                                                                |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| People                     | The generated Confluence page tree - see [Documentation generator](documentation-generator.md)                                                                                                     |
| Dashboards and inventories | The public read endpoints under `/api/model`                                                                                                                                                       |
| Governance                 | `GET /api/model/rest-api-relation-without-pact` and `GET /api/model/system-components-without-open-api-spec` name the REST relations with no contract test and the services that published no spec |
| Other services             | `/external-api/dbschemas`, behind a semantic role - see [Security](security.md)                                                                                                                    |

## Related

- [Architecture](architecture.md) - how the library is put together
- [Data model](data-model.md) - the entities and their relationships
- [Importers](importers.md) - the sources the inventory is built from
- [Operations](operations.md) - running an instance and keeping it healthy
