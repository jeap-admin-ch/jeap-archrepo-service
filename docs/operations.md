# Operations

What an instance does while it runs, how to tell whether it is still doing it, and the few things that have to be
maintained by hand.

## The model update job

`UpdateService` in `jeap-archrepo-web` carries one scheduled job.

| Job             | Cron property              | Lock name           |
| --------------- | -------------------------- | ------------------- |
| `updateModel()` | `archrepo.update-schedule` | `update-model-task` |

The cron expression is **required** - the application does not start without it. Set it to `-` to disable the job.

`updateModel()` loads the model, runs every registered importer sorted by `getOrder()`, calls
`ArchitectureModel.cleanup()` and saves the model.

### In a cluster

`ArchRepoApplication` enables ShedLock with `defaultLockAtMostFor = "10m"`, and the job declares
`lockAtLeastFor = "5s"`, `lockAtMostFor = "2h"`. The locks are rows in the `shedlock` table, created by the Flyway
migrations, so only one instance of a cluster runs the job even though every instance schedules it.

The scheduled method starts with `LockAssert.assertLocked()`: it never runs unlocked, which also covers the manual
job trigger below, since the lock is applied by a method interceptor rather than by the scheduler.

If an instance dies mid-run, the lock is held for at most `lockAtMostFor` - two hours - before another instance may
pick the job up again.

### The job is transactional

`updateModel()` runs in one transaction and saves once, at the end. An importer that throws therefore **rolls the
whole run back**: nothing an earlier importer found is persisted either, and the model stays exactly as the
previous successful run left it. That is the intended behaviour - a half-imported model would look like elements
had disappeared from their source and get them removed - but it means one broken source blocks the whole update,
so a failing importer is worth an alert rather than a shrug.

## Monitoring

The job is tracked by a `SchedulerRunTracker`, which persists the timestamp of the last **successful** run in the
`scheduler_run` table and registers a Micrometer gauge:

| Gauge                                 | Job           | Value                              |
| ------------------------------------- | ------------- | ---------------------------------- |
| `archrepo_model_update_last_run_from` | `update-model` | Days since the last successful run |

The value is in **days**, not seconds, so a daily schedule should keep the gauge at `0` or `1`. Because the
timestamp is persisted, the gauge survives a restart and does not reset to a healthy value when an instance is
redeployed. Alert on it rising above the job's period - that is the only signal that distinguishes "the scheduler
stopped" from "nothing changed".

The method is also `@Timed`, which yields the `archrepo_model_update` timer for run duration.

## Triggering a job by hand

| Request                                            | Effect                                                                       |
| -------------------------------------------------- | ---------------------------------------------------------------------------- |
| `POST /api/jobs` with `{ "type": "UPDATE_MODEL" }` | A full import run                                                            |
| `POST /api/jobs/import/{importerName}`             | One importer only, named by its **simple class name**, matched ignoring case |

Both are `@Async`: the response says the job started, not that it finished. Watch the log for the outcome.

Running a single importer loads and saves the model like a full run, but it does **not** call
`ArchitectureModel.cleanup()` - use it to refresh one source, not as a substitute for the scheduled run.

Both need the HTTP basic credentials, see [Security](security.md).

## Existing Confluence documentation

The new architecture documentation is already available from the jEAP Doc Service.
After deployment, the existing Confluence documentation is deliberately left in place and will become stale.
It is manually removed one week after deployment; this service no longer refreshes or cleans up those pages.

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
them in directly in the database to add prose to the architecture documentation produced by the Doc Service.

## How elements disappear again

Removal is not one mechanism but several, which is worth knowing when something lingers or vanishes unexpectedly.

| What                                    | Removed by                                                                            | When                                                                               |
| --------------------------------------- | ------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| A component the deployment log created  | `DeploymentlogSystemComponentImporter`, at the end of its run                         | The deployment log no longer reports it at all - one still reported as undeployed from this environment is marked `undeployed` instead and kept |
| A component the metrics created         | `AWSSystemComponentImporter` / `RhosSystemComponentImporter`, at the end of their run | Its `lastSeen` is more than 14 days old                                            |
| A REST relation                         | `ArchitectureModel.cleanup()`, at the end of a full run                               | It was imported and its `lastSeen` is more than 3 months old, or it was never seen |
| A `RestApi`                             | `ArchitectureModel.cleanup()`, at the end of a full run                               | No relation references it any more                                                 |
| An event, a command, or a Pact relation | Its own importer, at the **start** of its run                                         | Always - see below                                                                 |

Removing a component cascades: its relations, REST APIs, OpenAPI spec and database schema go with it.

The message type importer and the Pact broker importer do not diff at all. Each begins by calling
`removeAllByImporter()` for its own `Importer` value and then re-imports everything from scratch, so what its
source no longer reports is gone by construction. Because the whole run is one transaction, that wipe is only
visible if the re-import also succeeds.

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
