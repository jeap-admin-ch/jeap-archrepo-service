# Operations

What an instance does while it runs, how to tell whether it is still doing it, and the few things that have to be
maintained by hand.

## The two jobs

`UpdateService` in `jeap-archrepo-web` carries both scheduled jobs.

| Job                       | Cron property                                      | Lock name                     |
| ------------------------- | -------------------------------------------------- | ----------------------------- |
| `updateModel()`           | `archrepo.update-schedule`                         | `update-model-task`           |
| `generateDocumentation()` | `archrepo.documentation-generator.update-schedule` | `generate-documentation-task` |

Both cron expressions are **required** - the application does not start without them. Set a schedule to `-` to
disable a job.

`updateModel()` loads the model, runs every registered importer sorted by `getOrder()`, calls
`ArchitectureModel.cleanup()` and saves the model. `generateDocumentation()` loads the model and renders it to
Confluence without changing it, so a failing export never damages the inventory.

### In a cluster

`ArchRepoApplication` enables ShedLock with `defaultLockAtMostFor = "10m"`, and both jobs declare
`lockAtLeastFor = "5s"`, `lockAtMostFor = "2h"`. The locks are rows in the `shedlock` table, created by the Flyway
migrations, so only one instance of a cluster runs a job even though every instance schedules it.

Both methods start with `LockAssert.assertLocked()`: they never run unlocked, which also covers the manual
triggers below, since the lock is applied by a method interceptor rather than by the scheduler.

If an instance dies mid-run, the lock is held for at most `lockAtMostFor` - two hours - before another instance may
pick the job up again.

### Both jobs are transactional

`updateModel()` runs in one transaction and saves once, at the end. An importer that throws therefore **rolls the
whole run back**: nothing an earlier importer found is persisted either, and the model stays exactly as the
previous successful run left it. That is the intended behaviour - a half-imported model would look like elements
had disappeared from their source and get them removed - but it means one broken source blocks the whole update,
so a failing importer is worth an alert rather than a shrug.

Some importers guard against that themselves. The reaction observer importers, for instance, catch and log a
failure per component or message type, so one graph the observer cannot deliver does not take the run down. That
is a property of the individual importer, not of the framework.

## Monitoring

Each job is tracked by a `SchedulerRunTracker`, which persists the timestamp of the last **successful** run in the
`scheduler_run` table and registers a Micrometer gauge:

| Gauge                                           | Job                      | Value                              |
| ----------------------------------------------- | ------------------------ | ---------------------------------- |
| `archrepo_model_update_last_run_from`           | `update-model`           | Days since the last successful run |
| `archrepo_generate_documentation_last_run_from` | `generate-documentation` | Days since the last successful run |

The value is in **days**, not seconds, so a daily schedule should keep the gauge at `0` or `1`. Because the
timestamp is persisted, the gauge survives a restart and does not reset to a healthy value when an instance is
redeployed. Alert on it rising above the job's period - that is the only signal that distinguishes "the scheduler
stopped" from "nothing changed".

Both methods are also `@Timed`, which yields the timers `archrepo_model_update` and
`archrepo_generate_documentation` for run durations.

## Triggering a job by hand

| Request                                            | Effect                                                                       |
| -------------------------------------------------- | ---------------------------------------------------------------------------- |
| `POST /api/jobs` with `{ "type": "UPDATE_MODEL" }` | A full import run                                                            |
| `POST /api/jobs` with `{ "type": "GENERATE_DOC" }` | A full Confluence export                                                     |
| `POST /api/jobs/import/{importerName}`             | One importer only, named by its **simple class name**, matched ignoring case |

All three are `@Async`: the response says the job started, not that it finished. Watch the log for the outcome.

Running a single importer loads and saves the model like a full run, but it does **not** call
`ArchitectureModel.cleanup()` - use it to refresh one source, not as a substitute for the scheduled run.

All three need the HTTP basic credentials, see [Security](security.md).

## What the runtime image needs

The documentation generator renders the reaction graphs by piping DOT into an external **Graphviz** process
(`dot -Tpng`, with a 60 second timeout per graph). The `dot` binary therefore has to be on the `PATH` of the
container the instance runs in; without it every graph attachment fails.

The PlantUML diagrams are different: they are written into the page as a Confluence `plantuml` macro and rendered
by Confluence, so the target Confluence instance needs that macro available. See
[Documentation generator](documentation-generator.md).

## Maintenance by hand

Almost everything is imported. Four things are not.

**Creating a system.** Importers file components into systems that already exist and skip everything else, so a
new system has to be created first:

```http
POST /api/management/system
Content-Type: application/json

{
  "name": "foo",
  "description": "The foo system",
  "confluenceLink": "https://example.com/wiki/foo",
  "aliases": ["fo"],
  "teamName": "team-blue"
}
```

`name` and `teamName` are required; the team is created if it does not exist yet. The call fails with `400` when
the name or alias collides with an existing system. Answer is `201`.

**Creating a team** with its contact details: `POST /api/management/team`, with `name` and the optional
`contactAddress`, `confluenceLink` and `jiraLink` as **query parameters**, not as a body.

**Removing a wrong REST relation**, for instance one a superseded Pact contract created:

```http
DELETE /api/management/rest-api
Content-Type: application/json

{
  "providerName": "foo-bar-service",
  "consumerName": "foo-baz-service",
  "method": "GET",
  "path": "/api/bar/{id}"
}
```

All four fields are required. The relation is marked `DELETED` rather than removed, so a later import does not
simply recreate it. `404` when nothing matches.

**Descriptions.** `System`, `SystemComponent` and `MessageType` each have a `description` no importer sets. Fill
them in directly in the database to add prose to the generated pages.

## How elements disappear again

Removal is not one mechanism but several, which is worth knowing when something lingers or vanishes unexpectedly.

| What                                    | Removed by                                                                            | When                                                                               |
| --------------------------------------- | ------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| A component the deployment log created  | `DeploymentlogSystemComponentImporter`, at the end of its run                         | It is no longer among the deployed components                                      |
| A component the metrics created         | `AWSSystemComponentImporter` / `RhosSystemComponentImporter`, at the end of their run | Its `lastSeen` is more than 14 days old                                            |
| A REST relation                         | `ArchitectureModel.cleanup()`, at the end of a full run                               | It was imported and its `lastSeen` is more than 3 months old, or it was never seen |
| A `RestApi`                             | `ArchitectureModel.cleanup()`, at the end of a full run                               | No relation references it any more                                                 |
| An event, a command, or a Pact relation | Its own importer, at the **start** of its run                                         | Always - see below                                                                 |

Removing a component cascades: its relations, REST APIs, OpenAPI spec and database schema go with it.

The message type importer and the Pact broker importer do not diff at all. Each begins by calling
`removeAllByImporter()` for its own `Importer` value and then re-imports everything from scratch, so what its
source no longer reports is gone by construction. Because the whole run is one transaction, that wipe is only
visible if the re-import also succeeds.

Confluence pages are cleaned up separately, by the export - see
[Documentation generator](documentation-generator.md).

## One-off work on the first start of 11.0.0

The `V2_6_0` migration fills the `content_hash` of every OpenAPI spec and database schema stored before the column
existed. It is a single pass over both tables, reading each artifact once, and it runs like any other migration -
before the service accepts requests, under Flyway's lock, recorded in `flyway_schema_history`. On a large
landscape the first start of this version therefore takes noticeably longer; subsequent starts do no such work.

## Related

- [Getting started](getting-started.md) - bringing an instance up
- [Configuration](configuration.md) - the schedules and the source properties
- [Importers](importers.md) - what each run does
- [API](api.md) - the endpoints used here
- [Documentation generator](documentation-generator.md) - the export job
