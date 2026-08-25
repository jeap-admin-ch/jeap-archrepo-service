# API

The arch repo service publishes its model over REST, and receives OpenAPI specs and database schemas the same
way. This page documents that API - the one that existed before the docs API was added.

There are two roots:

| Root               | What lives there                                                                               |
| ------------------ | ---------------------------------------------------------------------------------------------- |
| `/api/**`          | The model, the artifact push and read endpoints, the job triggers and the management endpoints |
| `/external-api/**` | The one resource meant for a consumer outside the platform: the database schema of a component |

A third root, `/docs-api/**`, is the segregated read API for the jEAP Doc Service and is documented separately -
see [Docs API](docs-api.md). It shares nothing with this page: neither its authentication, nor its payloads, nor
its error format.

The OpenAPI description is served by the jEAP Swagger starter and grouped by root, in `OpenApiConfig` and
`DocsApiConfiguration`:

| Group                                | Paths                                        |
| ------------------------------------ | -------------------------------------------- |
| Internal Architecture Repository API | `/api/**`                                    |
| External Architecture Repository API | `/external-api/**`                           |
| Architecture Repository Docs API     | `/docs-api/**` - see [Docs API](docs-api.md) |

## The operations

| Method and resource                                      | Purpose                                                                    |
| -------------------------------------------------------- | -------------------------------------------------------------------------- |
| `GET /api/model`                                         | All systems with their components, without relations                       |
| `GET /api/model/{system}/relations`                      | Every active relation - REST, event, command - with one end in a system    |
| `GET /api/model/rest-api-relation-without-pact`          | Governance query: active REST relations with no Pact contract              |
| `GET /api/model/system-components-without-open-api-spec` | Governance query: backend services and self-contained systems with no spec |
| `POST /api/openapi/{component}`                          | Push a component's OpenAPI spec                                            |
| `POST /api/openapi/{system}/{component}`                 | The same handler; the system segment is ignored                            |
| `GET /api/openapi/{component}`                           | The stored spec of a component, verbatim                                   |
| `GET /api/openapi/{system}/{component}`                  | The same handler; the system segment is ignored                            |
| `GET /api/openapi/{component}/rest-apis`                 | The REST endpoints derived from the spec, with its version and server URL  |
| `GET /api/openapi/{system}/{component}/rest-apis`        | The same handler; the system segment is ignored                            |
| `GET /api/openapi/versions`                              | The spec version of every component that has one                           |
| `POST /api/dbschemas`                                    | Push a component's database schema                                         |
| `GET /api/dbschemas/versions`                            | The schema version of every component that has one                         |
| `GET /external-api/dbschemas`                            | The stored database schema of one component                                |
| `POST /api/jobs`                                         | Run the model update or the documentation generation now                   |
| `POST /api/jobs/import/{importerName}`                   | Run a single importer now                                                  |
| `POST /api/management/system`                            | Create a system that no importer can derive                                |
| `POST /api/management/team`                              | Create a team                                                              |
| `DELETE /api/management/rest-api`                        | Mark one REST API relation deleted                                         |

`{component}` is always matched by **name, ignoring case**, and never by an alias. `{system}` is matched by name,
ignoring case, only in `GET /api/model/{system}/relations`; in every path under `/api/openapi` the segment is
bound to a parameter the handler ignores.

## Authentication

Which mechanism applies is decided by the path prefix, on two filter chains - see [Security](security.md) for the
whole picture.

| Path                                         | Chain                                       | Credentials                                                  |
| -------------------------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| `/api/**`                                    | `WebSecurityConfig.apiSecurityFilterChain`  | HTTP basic as the user `api`, password `archrepo.api.secret` |
| everything else, `/external-api/**` included | the jEAP security starter's resource server | An OAuth2 bearer token, plus a semantic role                 |

On the basic chain these `GET`s are `permitAll()` and need no credentials at all:

- `GET /api/model`, `GET /api/model/{system}/relations`
- `GET /api/model/rest-api-relation-without-pact`, `GET /api/model/system-components-without-open-api-spec`
- `GET /api/openapi/**`, `GET /api/dbschemas/**`

Everything else under `/api/**` requires the basic credentials.

> The chain also permits `GET /api/reactions/**`, but **no controller serves that path** - it is a leftover
> matcher from an endpoint that no longer exists.

### The two pushes are the exception

`POST /api/openapi/**` and `POST /api/dbschemas` are cut out of the basic chain's security matcher **when the
request carries an `Authorization: Bearer ...` header**, and then fall to the resource-server chain instead. The
same request without that header stays on the basic chain. That is what lets a pushing pipeline use whichever
credential it has.

The two do not check the role the same way:

| Push                   | How the role is checked                                                            | Basic credentials alone         |
| ---------------------- | ---------------------------------------------------------------------------------- | ------------------------------- |
| `POST /api/openapi/**` | Programmatically, and only for a `JeapAuthenticationToken`: `openapidoc` / `write` | accepted - the check is skipped |
| `POST /api/dbschemas`  | `@PreAuthorize("hasRole('database-schema', 'write')")`                             | not sufficient, see below       |

The two-argument `hasRole(resource, operation)` exists only on the expression root jEAP builds for a
`JeapAuthenticationToken`; for any other authentication the handler falls back to Spring's default root, which has
no such method. `POST /api/dbschemas` is therefore a **bearer-token endpoint in practice**, even though the filter
chain would let the basic credentials through as far as the method-security check.

Semantic roles resolve to `<system-name>_@<resource>_#<operation>`, with `<system-name>` taken from
`jeap.security.oauth2.resourceserver.system-name`.

## Errors

Errors on these endpoints are **plain text**, not [RFC 9457](https://www.rfc-editor.org/info/rfc9457/) problem
documents. The advice that answers with `application/problem+json` is scoped to the docs API package; the
endpoints on this page keep the format their consumers already parse, which `ExistingApiUnchangedIT` pins down.

`GlobalExceptionHandler` maps four exceptions:

| Exception                         | Status                           | Raised by                                                      |
| --------------------------------- | -------------------------------- | -------------------------------------------------------------- |
| `MethodArgumentNotValidException` | `400`                            | A `@Valid` request body that fails its constraints             |
| `OpenApiFileParsingException`     | `400`                            | An uploaded OpenAPI spec that cannot be parsed                 |
| `OpenApiException`                | `400`                            | An unknown component, on the `/api/openapi` read endpoints     |
| `DatabaseSchemaException`         | the status the exception carries | The database schema endpoints; every factory method uses `500` |

Everything else is left to Spring: a `ResponseStatusException` - `404` for an unknown system, `400` for a system
that already exists - becomes the container's standard error response, and a `@Size` violation on a path variable
or a query parameter becomes a `400`.

## `GET /api/model`

Every system with its components, without relations. The payload other services consume, and the one under Pact
contract - which is why the docs API deliberately does not reuse it.

### Request

`GET /api/model`

No path parameters, no query parameters, no request body.

Authentication: none. The endpoint is `permitAll()`.

### Response

`200 OK`, `application/json`.

```json
{
  "systems": [
    {
      "name": "wvs",
      "description": "Warenverkehrssystem",
      "ownedBy": "Team Blue",
      "aliases": ["WVS-ALIAS"],
      "systemComponents": [
        {
          "name": "wvs-foo-bar-service",
          "description": "Handles foo and bar",
          "ownedBy": "Team Blue",
          "importer": "DEPLOYMENT_LOG",
          "type": "BACKEND_SERVICE"
        }
      ]
    }
  ]
}
```

| Field                            | Type            | Meaning                                                                                                                                   |
| -------------------------------- | --------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `systems`                        | array           | Every system of the landscape                                                                                                             |
| `systems[].name`                 | string          | The system's name                                                                                                                         |
| `systems[].description`          | string          | Free text from the importers; **omitted** when the system has none                                                                        |
| `systems[].ownedBy`              | string          | The name of the system's default owner; **omitted** when it has none                                                                      |
| `systems[].aliases`              | array of string | Further names the system is known under; an empty array when it has none                                                                  |
| `systems[].systemComponents`     | array           | The system's components                                                                                                                   |
| `systemComponents[].name`        | string          | The component's name, and what `{component}` matches everywhere else on this page                                                         |
| `systemComponents[].description` | string          | Free text from the importers, `null` when it has none                                                                                     |
| `systemComponents[].ownedBy`     | string          | The name of the component's own team, `null` when it has none                                                                             |
| `systemComponents[].importer`    | enum            | `GRAFANA`, `DEPLOYMENT_LOG`, `MESSAGE_TYPE_REGISTRY`, `PACT_BROKER`, `OPEN_API` or `REST_CONTROLLER`; `null` when no importer is recorded |
| `systemComponents[].type`        | enum            | `BACKEND_SERVICE`, `FRONTEND`, `GATEWAY`, `MOBILE_APP`, `SELF_CONTAINED_SYSTEM` or `UNKNOWN`                                              |

`SystemDto` is annotated `@JsonInclude(NON_NULL)`, so an absent system field is missing from the payload;
`SystemComponentDto` is not, so an absent component field is an explicit `null`.

| Status | When                                                    |
| ------ | ------------------------------------------------------- |
| `200`  | Always - an empty landscape is an empty `systems` array |

## `GET /api/model/{system}/relations`

Every **active** relation with one end in the named system, wherever it was defined.

### Request

`GET /api/model/{system}/relations`

| Path parameter | Matches                                                                |
| -------------- | ---------------------------------------------------------------------- |
| `system`       | The system's **name**, ignoring case. Aliases are **not** matched here |

No query parameters, no request body.

Authentication: none. The endpoint is `permitAll()`.

### Response

`200 OK`, `application/json`.

```json
[
  {
    "relationType": "REST_API_RELATION",
    "consumerSystem": "zoll",
    "consumer": "zoll-gateway",
    "providerSystem": "wvs",
    "provider": "wvs-foo-bar-service",
    "method": "GET",
    "path": "/api/foo/{id}"
  },
  {
    "relationType": "EVENT_RELATION",
    "consumerSystem": "zoll",
    "consumer": "zoll-gateway",
    "providerSystem": "wvs",
    "provider": "wvs-foo-bar-service",
    "messageType": "WvsDeclarationAcceptedEvent"
  }
]
```

| Field            | Type   | Meaning                                                                               |
| ---------------- | ------ | ------------------------------------------------------------------------------------- |
| `relationType`   | enum   | `REST_API_RELATION`, `EVENT_RELATION` or `COMMAND_RELATION`                           |
| `consumer`       | string | Name of the consuming component                                                       |
| `consumerSystem` | string | The system owning that component; omitted when the component is not part of the model |
| `provider`       | string | Name of the providing component                                                       |
| `providerSystem` | string | The system owning that component; omitted when the component is not part of the model |
| `method`         | string | HTTP method - REST relations only                                                     |
| `path`           | string | Path template - REST relations only                                                   |
| `messageType`    | string | The event or the command name - event and command relations only                      |

`RelationDto` is annotated `@JsonInclude(NON_NULL)`, so the fields that do not apply to a relation type are
**omitted**, not rendered as `null`.

| Status | When                                                                 |
| ------ | -------------------------------------------------------------------- |
| `200`  | The system exists; a system without relations answers an empty array |
| `404`  | No system of that name - an alias does not count                     |

## `GET /api/model/rest-api-relation-without-pact`

The governance query behind "which REST call is not covered by a contract": every **active** REST API relation of
the whole landscape whose `pactUrl` is null.

### Request

`GET /api/model/rest-api-relation-without-pact`

No path parameters, no query parameters, no request body.

Authentication: none. The endpoint is `permitAll()`.

### Response

`200 OK`, `application/json`.

```json
[
  {
    "consumerSystem": "zoll",
    "consumer": "zoll-gateway",
    "providerSystem": "wvs",
    "provider": "wvs-foo-bar-service",
    "method": "GET",
    "path": "/api/foo/{id}"
  }
]
```

| Field            | Type   | Meaning                                                                            |
| ---------------- | ------ | ---------------------------------------------------------------------------------- |
| `consumerSystem` | string | The system owning the consuming component, `null` when it is not part of the model |
| `consumer`       | string | Name of the consuming component                                                    |
| `providerSystem` | string | The system owning the providing component, `null` when it is not part of the model |
| `provider`       | string | Name of the providing component                                                    |
| `method`         | string | The HTTP method of the called endpoint                                             |
| `path`           | string | The path template of the called endpoint                                           |

`RestApiRelationDto` carries no `@JsonInclude`, so an unresolved system is an explicit `null` here - unlike in
`RelationDto`.

| Status | When                                         |
| ------ | -------------------------------------------- |
| `200`  | Always - nothing to report is an empty array |

## `GET /api/model/system-components-without-open-api-spec`

The other governance query: which components ought to have published an OpenAPI spec and have not.

### Request

`GET /api/model/system-components-without-open-api-spec`

No path parameters, no query parameters, no request body.

Authentication: none. The endpoint is `permitAll()`.

### Response

`200 OK`, `application/json` - a flat array of component names.

```json
["wvs-foo-bar-service", "zoll-gateway"]
```

Only components of type `BACKEND_SERVICE` or `SELF_CONTAINED_SYSTEM` are considered; a component is listed when
its system holds no `OpenApiSpec` naming it as the provider.

| Status | When                                     |
| ------ | ---------------------------------------- |
| `200`  | Always - full coverage is an empty array |

## `POST /api/openapi/{component}`

Push the OpenAPI spec of one component. `POST /api/openapi/{system}/{component}` reaches the **same handler**; its
`system` segment is bound to a parameter the handler ignores, so the two forms behave identically.

### Request

`POST /api/openapi/{component}`

| Path parameter | Matches                                                              |
| -------------- | -------------------------------------------------------------------- |
| `component`    | The component's **name**, ignoring case. At most 200 characters      |
| `system`       | Two-segment form only; accepted, at most 200 characters, and ignored |

| Query parameter | Required | Meaning                                                                                 |
| --------------- | -------- | --------------------------------------------------------------------------------------- |
| `version`       | no       | The version to record with the spec. At most 200 characters; stored as null when absent |

| Request header  | Required | Value                                                           |
| --------------- | -------- | --------------------------------------------------------------- |
| `Authorization` | yes      | HTTP basic, or a `Bearer` token carrying `openapidoc` / `write` |
| `Content-Type`  | yes      | `multipart/form-data`                                           |

The body is `multipart/form-data` with one required part:

| Part   | Required | Content                           |
| ------ | -------- | --------------------------------- |
| `file` | yes      | The OpenAPI document, JSON, as-is |

```bash
curl -u api:$ARCHREPO_API_SECRET \
     -F file=@openapi.json \
     "https://archrepo.example.com/my-archrepo-service/api/openapi/wvs-foo-bar-service?version=1.4.2"
```

What the handler does, in order:

1. If the request is authenticated with a bearer token, it checks `openapidoc` / `write` programmatically. Basic
   authentication skips that check.
2. It looks the component up by name, ignoring case. **If there is no such component the push is ignored** and
   answered `200` - no component, no system and no spec is created.
3. It **parses the uploaded bytes as JSON** to read `servers[0].url`. A body that does not parse, or that names no
   server, is rejected with `400` and **nothing is persisted**.
4. It stores the spec - creating it (`201`), or replacing the stored bytes, version and server URL (`200`).
5. It re-imports the component's REST APIs from the `paths` object of the spec, adding the ones that are new and
   dropping the ones only the `OPEN_API` importer still claimed.

The server URL is read from the spec on every push and stored with it, whether the spec is created or updated.

### Response

Empty body in every case.

| Status | When                                                                                                                  |
| ------ | --------------------------------------------------------------------------------------------------------------------- |
| `201`  | The component had no spec yet and one was stored                                                                      |
| `200`  | The component's spec was replaced - **or** the component does not exist and the push was ignored                      |
| `400`  | `component`, `system` or `version` longer than 200 characters, or a spec that is not parseable JSON with a server URL |
| `401`  | Neither basic credentials nor a bearer token                                                                          |
| `403`  | A bearer token without `openapidoc` / `write`                                                                         |
| `500`  | The upload could not be read - an `IOException` on `file.getBytes()`                                                  |

`200` is therefore ambiguous by design: it means "updated" and "ignored" alike. A caller that wants to know
whether the component is in the model has to ask `GET /api/model`.

## `GET /api/openapi/{component}`

The stored spec of a component, verbatim. `GET /api/openapi/{system}/{component}` is the same handler with the
system segment ignored.

### Request

`GET /api/openapi/{component}`

| Path parameter | Matches                                                              |
| -------------- | -------------------------------------------------------------------- |
| `component`    | The component's **name**, ignoring case. At most 200 characters      |
| `system`       | Two-segment form only; accepted, at most 200 characters, and ignored |

No query parameters, no request body.

Authentication: none. `GET /api/openapi/**` is `permitAll()`.

### Response

`200 OK`. The body is the stored spec, decoded as UTF-8 and written unchanged - no envelope and no field of the
arch repo's own. The handler returns a `String` and declares no `produces`, so the media type is negotiated with
the request rather than fixed to `application/json`.

```json
{
  "openapi": "3.0.3",
  "info": { "title": "wvs-foo-bar-service", "version": "1.4.2" },
  "servers": [ { "url": "https://foo-bar.example.com" } ],
  "paths": {
    "/api/foo/{id}": {
      "get": {
        "operationId": "getFoo",
        "responses": { "200": { "description": "The foo" } }
      }
    }
  }
}
```

| Status | When                                                                                                |
| ------ | --------------------------------------------------------------------------------------------------- |
| `200`  | The component exists and has a spec                                                                 |
| `400`  | No component of that name - plain text, `SystemComponent ... does not exists in architecture model` |
| `400`  | `component` or `system` longer than 200 characters                                                  |
| `404`  | The component exists but has published no spec - plain text, `No Open API spec found for ...`       |

An unknown component answering `400` rather than `404` is a quirk of this endpoint: the miss is raised as an
`OpenApiException`, which `GlobalExceptionHandler` maps to `400`.

## `GET /api/openapi/{component}/rest-apis`

The REST endpoints the arch repo derived from a component's spec, with the version and server URL it recorded.
`GET /api/openapi/{system}/{component}/rest-apis` is the same handler with the system segment ignored.

### Request

`GET /api/openapi/{component}/rest-apis`

| Path parameter | Matches                                                                |
| -------------- | ---------------------------------------------------------------------- |
| `component`    | The component's **name**, ignoring case. At most 200 characters        |
| `system`       | Three-segment form only; accepted, at most 200 characters, and ignored |

No query parameters, no request body.

Authentication: none. `GET /api/openapi/**` is `permitAll()`.

### Response

`200 OK`, `application/json`.

```json
{
  "serverUrl": "https://foo-bar.example.com",
  "lastUpdated": "2026-08-12T05:31:00Z",
  "version": "1.4.2",
  "restApis": [
    { "method": "get", "path": "/api/foo/{id}" },
    { "method": "post", "path": "/api/foo" }
  ]
}
```

| Field               | Type               | Meaning                                                                                                                        |
| ------------------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| `serverUrl`         | string             | The first entry of the spec's `servers`, recorded on every push                                                                |
| `lastUpdated`       | ISO-8601 timestamp | When the spec was last written: its modification time, or its creation time if never modified                                  |
| `version`           | string             | The version the push carried, `null` when it carried none                                                                      |
| `restApis`          | array              | The endpoints of the component that carry the `OPEN_API` importer                                                              |
| `restApis[].method` | string             | The HTTP method as the model stores it - the spec's method key, so lower case, unless another importer created the entry first |
| `restApis[].path`   | string             | The path template                                                                                                              |

Endpoints the model knows only from another source - Grafana, the Pact broker, a REST controller scan - are
filtered out; only those the OpenAPI importer claims are listed. A REST API another importer found first and the
OpenAPI importer then confirmed keeps the method spelling of that first importer.

| Status | When                                                                    |
| ------ | ----------------------------------------------------------------------- |
| `200`  | The component exists and has a spec                                     |
| `400`  | No component of that name, or a path segment longer than 200 characters |
| `404`  | The component exists but has published no spec - empty body             |

## `GET /api/openapi/versions`

Which spec version each component last published. The literal path wins over the `/{component}` template, so a
component cannot be read under the name `versions`.

### Request

`GET /api/openapi/versions`

No path parameters, no query parameters, no request body.

Authentication: none. `GET /api/openapi/**` is `permitAll()`.

### Response

`200 OK`, `application/json`.

```json
[
  { "system": "wvs", "component": "wvs-foo-bar-service", "version": "1.4.2" },
  { "system": "zoll", "component": "zoll-gateway", "version": null }
]
```

| Field       | Type   | Meaning                                                   |
| ----------- | ------ | --------------------------------------------------------- |
| `system`    | string | The system that defines the spec                          |
| `component` | string | The component the spec belongs to                         |
| `version`   | string | The version the push carried, `null` when it carried none |

One entry per stored spec, and there is exactly one spec per component. The query declares no `order by`, so the
order is whatever the database returns.

| Status | When                                        |
| ------ | ------------------------------------------- |
| `200`  | Always - no spec anywhere is an empty array |

## `POST /api/dbschemas`

Push the database schema of one component: the tables, columns, primary key and foreign keys an ERD is generated
from.

### Request

`POST /api/dbschemas`

No path parameters and no query parameters.

| Request header  | Required | Value                                                 |
| --------------- | -------- | ----------------------------------------------------- |
| `Authorization` | yes      | A `Bearer` token carrying `database-schema` / `write` |
| `Content-Type`  | yes      | `application/json` - the handler declares `consumes`  |

The body is a `CreateOrUpdateDbSchemaDto` and is validated with `@Valid`:

```json
{
  "systemComponentName": "wvs-foo-bar-service",
  "schema": {
    "name": "wvs_foo_bar",
    "version": "42",
    "tables": [
      {
        "name": "declaration",
        "columns": [
          { "name": "id", "type": "uuid", "nullable": false },
          { "name": "consignee_id", "type": "uuid", "nullable": true }
        ],
        "primaryKey": { "name": "declaration_pkey", "columnNames": ["id"] },
        "foreignKeys": [
          {
            "name": "declaration_consignee_fk",
            "columnNames": ["consignee_id"],
            "referencedTableName": "consignee",
            "referencedColumnNames": ["id"]
          }
        ]
      }
    ]
  }
}
```

| Field                  | Required | Meaning                                                                            |
| ---------------------- | -------- | ---------------------------------------------------------------------------------- |
| `systemComponentName`  | yes      | The component the schema belongs to, matched by name, ignoring case                |
| `schema`               | yes      | The `DatabaseSchema` record of `jeap-archrepo-dbschema-model`                      |
| `schema.name`          | yes      | The database schema's own name                                                     |
| `schema.version`       | yes      | The schema version the publisher wrote                                             |
| `schema.tables`        | yes      | At least one table                                                                 |
| `tables[].name`        | yes      | Table name                                                                         |
| `tables[].columns`     | yes      | At least one column, each with `name`, `type` and `nullable`                       |
| `tables[].primaryKey`  | no       | `name` and a non-empty `columnNames`                                               |
| `tables[].foreignKeys` | no       | Each with `name`, `columnNames`, `referencedTableName` and `referencedColumnNames` |

Unlike the OpenAPI push, this one **creates the component when it does not exist** - and, when the system named by
the part of the component name before the first `-` is unknown as well, that system plus a team of the same name.
It is the only endpoint that does so; see [Concepts](concepts.md). A component created this way is recorded with
the importer `REST_CONTROLLER`.

The push replaces the component's single schema in place, or creates it. Two concurrent creations are handled: the
loser of the unique constraint retries as an update in a new transaction.

### Response

Empty body in every case.

| Status | When                                                                                                            |
| ------ | --------------------------------------------------------------------------------------------------------------- |
| `201`  | The component had no schema yet and one was stored                                                              |
| `200`  | The component's schema was replaced                                                                             |
| `400`  | The body fails its constraints - a missing `tables`, an empty column name, and so on                            |
| `401`  | No bearer token                                                                                                 |
| `403`  | A token without `database-schema` / `write`                                                                     |
| `500`  | The schema could not be serialized, or any other unexpected failure - plain text from `DatabaseSchemaException` |

## `GET /api/dbschemas/versions`

Which schema version each component last published - the counterpart of `GET /api/openapi/versions`.

### Request

`GET /api/dbschemas/versions`

No path parameters, no query parameters, no request body.

Authentication: none. `GET /api/dbschemas/**` is `permitAll()`.

### Response

`200 OK`, `application/json`.

```json
[
  { "system": "wvs", "component": "wvs-foo-bar-service", "version": "42" },
  { "system": "zoll", "component": "zoll-gateway", "version": "7" }
]
```

| Field       | Type   | Meaning                             |
| ----------- | ------ | ----------------------------------- |
| `system`    | string | The system the schema is linked to  |
| `component` | string | The component the schema belongs to |
| `version`   | string | The schema version as published     |

One entry per stored schema, and there is exactly one schema per component. The query declares no `order by`.

| Status | When                                          |
| ------ | --------------------------------------------- |
| `200`  | Always - no schema anywhere is an empty array |

## `GET /external-api/dbschemas`

The stored database schema of one component, for a consumer outside the platform. The only resource under
`/external-api/**`, and the reason that root exists: it sits on the resource-server chain and carries its own
role.

### Request

`GET /external-api/dbschemas?systemComponentName=wvs-foo-bar-service`

No path parameters.

| Query parameter       | Required | Meaning                                                              |
| --------------------- | -------- | -------------------------------------------------------------------- |
| `systemComponentName` | yes      | The component whose schema to return, matched by name, ignoring case |

| Request header  | Required | Value                                                         |
| --------------- | -------- | ------------------------------------------------------------- |
| `Authorization` | yes      | A `Bearer` token carrying `external-database-schema` / `read` |

No request body.

### Response

`200 OK`, `application/json`.

```json
{
  "systemComponentName": "wvs-foo-bar-service",
  "schema": {
    "name": "wvs_foo_bar",
    "version": "42",
    "tables": [
      {
        "name": "declaration",
        "columns": [
          { "name": "id", "type": "uuid", "nullable": false }
        ],
        "primaryKey": { "name": "declaration_pkey", "columnNames": ["id"] },
        "foreignKeys": null
      }
    ]
  }
}
```

| Field                 | Type   | Meaning                                                                      |
| --------------------- | ------ | ---------------------------------------------------------------------------- |
| `systemComponentName` | string | Echoed back **as the caller spelled it**, not as the component is stored     |
| `schema`              | object | The `DatabaseSchema` deserialized from the stored bytes and serialized again |

`DatabaseSchema` and its nested types carry no `@JsonInclude(NON_NULL)`, so a table without a primary key or
without foreign keys renders those as an explicit `null`.

| Status | When                                                                                     |
| ------ | ---------------------------------------------------------------------------------------- |
| `200`  | The component exists and has a schema                                                    |
| `401`  | No bearer token, or one the resource server rejects - basic credentials do not help here |
| `403`  | A token without `external-database-schema` / `read`                                      |
| `404`  | No component of that name, **or** the component has published no schema - empty body     |
| `500`  | The stored bytes could not be deserialized - plain text from `DatabaseSchemaException`   |

The two `404` cases are deliberately not told apart; this endpoint has no way to say "the component exists but has
nothing to show".

## `POST /api/jobs`

Run one of the two scheduled jobs now instead of waiting for its cron expression - see
[Operations](operations.md).

### Request

`POST /api/jobs`

No path parameters and no query parameters.

| Request header  | Required | Value                                  |
| --------------- | -------- | -------------------------------------- |
| `Authorization` | yes      | HTTP basic - there is no public access |
| `Content-Type`  | yes      | `application/json`                     |

```json
{ "type": "UPDATE_MODEL" }
```

| Field  | Required | Meaning                                                                                  |
| ------ | -------- | ---------------------------------------------------------------------------------------- |
| `type` | yes      | `UPDATE_MODEL` for a full import run, `GENERATE_DOC` for a full documentation generation |

### Response

`200 OK`, empty body.

The handler is `@Async`: the response says the job **started**, not that it finished, and a failure during the run
appears only in the log. The job still takes its ShedLock lock, so a cluster runs it once.

| Status | When                                                               |
| ------ | ------------------------------------------------------------------ |
| `200`  | The job was handed to the executor                                 |
| `400`  | The body is not valid JSON, or `type` is not one of the two values |
| `401`  | No basic credentials                                               |

## `POST /api/jobs/import/{importerName}`

Run a single importer, to refresh one source without a full run.

### Request

`POST /api/jobs/import/{importerName}`

| Path parameter | Matches                                                                                          |
| -------------- | ------------------------------------------------------------------------------------------------ |
| `importerName` | The importer's **simple class name**, matched ignoring case - `PactBrokerImporter`, for instance |

No query parameters and no request body.

| Request header  | Required | Value                                  |
| --------------- | -------- | -------------------------------------- |
| `Authorization` | yes      | HTTP basic - there is no public access |

### Response

`200 OK`, empty body.

`@Async` like `POST /api/jobs`. The run loads and saves the model as a full run does, but it does **not** call
`ArchitectureModel.cleanup()`. An unknown importer name fails on the async thread and is visible only in the log -
the response is still `200`.

| Status | When                                  |
| ------ | ------------------------------------- |
| `200`  | The import was handed to the executor |
| `401`  | No basic credentials                  |

## `POST /api/management/system`

Create a system that no importer can derive.

### Request

`POST /api/management/system`

No path parameters and no query parameters.

| Request header  | Required | Value                                  |
| --------------- | -------- | -------------------------------------- |
| `Authorization` | yes      | HTTP basic - there is no public access |
| `Content-Type`  | yes      | `application/json`                     |

```json
{
  "name": "wvs",
  "description": "Warenverkehrssystem",
  "confluenceLink": "https://confluence.example.com/display/WVS",
  "aliases": ["WVS-ALIAS"],
  "teamName": "Team Blue"
}
```

| Field            | Required | Meaning                                                                                    |
| ---------------- | -------- | ------------------------------------------------------------------------------------------ |
| `name`           | yes      | The system's name                                                                          |
| `description`    | no       | Free text                                                                                  |
| `confluenceLink` | no       | The system's Confluence space                                                              |
| `aliases`        | no       | Further names the system is to be known under                                              |
| `teamName`       | yes      | The default owner. An existing team of that name is reused, otherwise a new one is created |

`CreateSystemDto` declares `@NotNull` on `name` and `teamName`, but the handler does **not** annotate the body
with `@Valid`, so those constraints are not enforced on the request.

### Response

`201 Created`, empty body.

| Status | When                                                                                              |
| ------ | ------------------------------------------------------------------------------------------------- |
| `201`  | The system was created                                                                            |
| `400`  | A system with that **name or alias** already exists - `System with name or alias already exists.` |
| `401`  | No basic credentials                                                                              |

## `POST /api/management/team`

Create a team. Its parameters are **query parameters**, not a JSON body - the only write endpoint on this page
shaped that way.

### Request

`POST /api/management/team`

No path parameters and no request body.

| Query parameter  | Required           | Meaning                     |
| ---------------- | ------------------ | --------------------------- |
| `name`           | yes, but see below | The team's name             |
| `contactAddress` | no                 | The team's contact address  |
| `confluenceLink` | no                 | The team's Confluence space |
| `jiraLink`       | no                 | The team's Jira project     |

`name` is an un-annotated handler parameter, so the binder resolves it as an **optional** request parameter rather
than a required one. A call without it does not fail at binding; it fails when the transaction commits, because
`Team.name` is `@NotNull`.

| Request header  | Required | Value                                  |
| --------------- | -------- | -------------------------------------- |
| `Authorization` | yes      | HTTP basic - there is no public access |

```bash
curl -u api:$ARCHREPO_API_SECRET -X POST \
     "https://archrepo.example.com/my-archrepo-service/api/management/team?name=Team+Blue&contactAddress=team-blue@example.com"
```

### Response

`200 OK`, empty body - the handler returns `void`.

It always **saves a new team**: there is no lookup and no uniqueness check, so calling it twice with the same name
is not an update.

| Status | When                                                               |
| ------ | ------------------------------------------------------------------ |
| `200`  | The team was saved                                                 |
| `401`  | No basic credentials                                               |
| `500`  | `name` was omitted - the `@NotNull` of `Team.name` fails at commit |

## `DELETE /api/management/rest-api`

Mark one REST API relation deleted - the manual correction for a relation an importer got wrong and will not
retract by itself.

### Request

`DELETE /api/management/rest-api`

No path parameters and no query parameters.

| Request header  | Required | Value                                  |
| --------------- | -------- | -------------------------------------- |
| `Authorization` | yes      | HTTP basic - there is no public access |
| `Content-Type`  | yes      | `application/json`                     |

```json
{
  "providerName": "wvs-foo-bar-service",
  "consumerName": "zoll-gateway",
  "method": "GET",
  "path": "/api/foo/{id}"
}
```

| Field          | Required | Meaning                                  |
| -------------- | -------- | ---------------------------------------- |
| `providerName` | yes      | The providing component, matched exactly |
| `consumerName` | yes      | The consuming component, matched exactly |
| `method`       | yes      | The HTTP method, matched ignoring case   |
| `path`         | yes      | The path template, matched ignoring case |

The body is annotated `@Valid`, so a missing field is a `400` before the handler runs. Only relations whose status
is `ACTIVE` are searched, and the first match is marked deleted - the row stays, its status changes.

### Response

`200 OK` with the plain-text body `Relation deleted successfully`.

| Status | When                                                                                             |
| ------ | ------------------------------------------------------------------------------------------------ |
| `200`  | A matching active relation was found and marked deleted                                          |
| `400`  | A field of the body is null                                                                      |
| `401`  | No basic credentials                                                                             |
| `404`  | No active relation between the two components matches the method and path - `No relations found` |

## Related

- [Docs API](docs-api.md) - the segregated read API for the doc service, which changes nothing here
- [Security](security.md) - the two authentication mechanisms and all roles
- [Architecture](architecture.md) - the model behind the API
- [Data model](data-model.md) - the entities the payloads map onto
- [Concepts](concepts.md) - what a system, a component and a relation are
- [Operations](operations.md) - the management and job endpoints in context
- [Importers](importers.md) - the pushed sources
