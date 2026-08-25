# Importers

An importer reads one external source and writes what it finds into the architecture model. Importers are the
extension point of the arch repo service: the library ships several, and an instance can add its own.

## The extension point

```java
public interface ArchRepoImporter {

    default int getOrder() {
        return Integer.MAX_VALUE - 100;
    }

    void importIntoModel(ArchitectureModel architectureModel, String environment);
}
```

`UpdateService` injects every `ArchRepoImporter` bean, sorts them by `getOrder()` **ascending**, and calls each one
with the same in-memory model. Nothing is written to the database in between - the caller saves the model once,
after the last importer has run.

`environment` is `archrepo-config.environment` lowercased, and is passed so that importers reading
environment-specific data (metrics, deployment versions) query the right stage.

### Ordering

The default order is `Integer.MAX_VALUE - 100`, so an importer that does not care runs late. Importers that
**create system components** pin an explicit low value, because when two sources describe the same component the
one that runs last decides what it looks like:

| Order                     | Importer                                                    | Why                                                                                                                                                                  |
| ------------------------- | ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Integer.MIN_VALUE`       | `DeploymentlogSystemComponentImporter`                      | Runs first: a component that was deployed                                                                                                                            |
| `Integer.MIN_VALUE + 50`  | `AWSSystemComponentImporter`, `RhosSystemComponentImporter` | Runs after the deployment log and takes precedence, because metrics are a live signal from a running component while a deployment only says something was rolled out |
| `Integer.MIN_VALUE + 100` | `AWSJeapRelationImporter`, `RhosJeapRelationImporter`       | Relations, once the components they connect exist                                                                                                                    |
| default                   | message type, Pact broker, reaction observer graphs         | Independent of component creation                                                                                                                                    |

### Conditional registration

There is **no hard-coded importer list**. Each `jeap-archrepo-importer-*` module carries an `@AutoConfiguration`
that registers its beans only when its configuration is present, so an instance activates an importer by
configuring it and nothing else:

| Importer module     | Active when                                    | Imports                                                                                                                 |
| ------------------- | ---------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `deploymentlog`     | `deploymentlog.url` is set                     | Systems and components from the jEAP deployment log, with their deployed versions                                       |
| `prometheus-aws`    | `prometheus-aws.host` is set                   | Components and their jEAP relations from an AWS-managed Prometheus workspace                                            |
| `prometheus-rhos`   | `prometheus-rhos.hosts` is a non-empty list    | The same from Prometheus instances on RHOS                                                                              |
| `messagetype`       | `messages.message-contract-service-uri` is set | Events, commands, their versions and their contracts, from the message type registries and the message contract service |
| `pactbroker`        | `pactbroker.url` is set                        | REST API relations, and the Pact contract that proves them                                                              |
| `reaction-observer` | `reactionobserverservice.url` is set           | The system, component and message reaction graphs                                                                       |
| `openapi`           | always                                         | Not an `ArchRepoImporter`: it parses specs pushed in over the REST API - see below                                      |

See [Configuration](configuration.md) for the full properties of each.

## Components go into systems that already exist

No importer creates a `System`. Each resolves a system name, looks it up as a name or alias, and **skips the
element with a warning** when there is no match. A system is created deliberately, through
`POST /api/management/system` - see [Operations](operations.md).

| Importer                          | Gets the system name from                                                       |
| --------------------------------- | ------------------------------------------------------------------------------- |
| `deploymentlog`, `prometheus-aws` | The component name up to the first `-`                                          |
| `prometheus-rhos`                 | The namespace, matched against `something-system-stage`                         |
| `pactbroker`                      | The middle segment of a `department-system-component` consumer or provider name |

## Provenance and removal

Every element an importer creates carries an `Importer` enum value naming its source - `GRAFANA`,
`DEPLOYMENT_LOG`, `MESSAGE_TYPE_REGISTRY`, `PACT_BROKER`, `OPEN_API` or `REST_CONTROLLER`. That is what lets an
importer delete what has disappeared from *its* source without touching what another source contributed.
Relations and REST APIs record a **set** of importers rather than one, so an element several sources found only
disappears when the last of them stops reporting it.

Two removal styles coexist:

- **Full replace.** The message type importer and the Pact broker importer call `removeAllByImporter()` for their
  own source at the *start* of the run and re-import everything.
- **Diff.** The deployment log importer removes its own components that were not in this run's response; the
  metrics importers remove their own components whose `lastSeen` is more than 14 days old (`isObsolete()`).

`ArchitectureModel.cleanup()`, which `UpdateService` calls after the last importer, is a third thing again and
concerns **only REST elements**: it removes imported `RestApiRelation`s not seen for three months and `RestApi`
entries no relation references any more. It does not touch components.

[Operations](operations.md) collects all of this in one table.

## Pushed sources

Not everything is polled. Components push two kinds of artifact into the model themselves:

| Artifact        | Endpoint                                  | Stored as                                                                                                                     |
| --------------- | ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| OpenAPI spec    | `POST /api/openapi/{systemComponentName}` | `OpenApiSpec`, one per component, updated in place. `OpenApiImporter` parses it and derives the component's `RestApi` entries |
| Database schema | `POST /api/dbschemas`                     | `SystemComponentDatabaseSchema`, one per component, updated in place                                                          |

Both are plain Spring components rather than `ArchRepoImporter` plugins, because they run inside the request. See
[API](api.md).

## Writing an importer

1. Implement `ArchRepoImporter` in the instance or in a new module.
2. Register it conditionally, so the instance opts in by configuration.
3. Set the `Importer` provenance on everything created, and refresh `lastSeen` on everything still seen, so that
   removal keeps working.
4. Give it an explicit `getOrder()` only if it creates elements another importer also creates.

## Related

- [Concepts](concepts.md) - the naming conventions the importers rely on
- [Architecture](architecture.md) - the model the importers write into
- [Data model](data-model.md) - the entities an importer fills
- [Configuration](configuration.md) - the properties that activate each importer
- [Operations](operations.md) - what a run does, and what happens when one fails
- [API](api.md) - the pushed sources, and how to trigger an import manually
