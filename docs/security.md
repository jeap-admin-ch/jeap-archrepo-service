# Security

The arch repo service protects its endpoints with **two different mechanisms**, on two different filter chains.
Which one applies depends only on the path.

| Path                                                        | Chain                                                           | Authentication                        | Authorization                                                   |
| ----------------------------------------------------------- | --------------------------------------------------------------- | ------------------------------------- | --------------------------------------------------------------- |
| `/api/**`                                                   | `WebSecurityConfig.apiSecurityFilterChain`, `@Order(100)`       | HTTP basic, one shared technical user | A role for writing endpoints; several read endpoints are public |
| everything else, i.e. `/external-api/**` and `/docs-api/**` | the jEAP security starter's resource-server chain, ordered last | OAuth2 bearer token                   | **Semantic roles**, checked per endpoint                        |

Two `POST`s are cut out of the basic-auth chain when they carry a bearer token, so that a pushing pipeline can use
whichever credential it has - but only one of them really accepts both:

- **`POST /api/openapi/**`** accepts either. `OpenApiController` checks the semantic role **programmatically**,
  guarded by `authentication instanceof JeapAuthenticationToken`, so a caller authenticated with basic auth skips
  the role check entirely.
- **`POST /api/dbschemas`** is annotated `@PreAuthorize("hasRole('database-schema', 'write')")`. The two-argument
  `hasRole` only resolves on the expression root the jEAP starter builds for a `JeapAuthenticationToken`, so basic
  credentials pass the filter chain but cannot satisfy the method-security check. In practice this endpoint needs
  a bearer token.

## HTTP basic on `/api/**`

`archrepo.api.secret` is the password of a single in-memory user holding the role `api`; the username defaults to
`archrepo-admin` and can be overridden via `archrepo.api.username`. Requests to `/api/**` that are not explicitly
permitted need it.

These endpoints are deliberately **public**, because they are read-only inventory queries consumed by dashboards:

- `GET /api/model`, `GET /api/model/{system}/relations`
- `GET /api/model/rest-api-relation-without-pact`, `GET /api/model/system-components-without-open-api-spec`
- `GET /api/dbschemas/**`, `GET /api/openapi/**`

Everything else under `/api/**` requires the basic credentials.

> The chain also permits `GET /api/reactions/**`, but no controller serves that path - it is a leftover matcher
> from an endpoint that no longer exists.

## OAuth2 and semantic roles

Outside `/api/**`, the jEAP security starter's chain applies: the request must carry a valid bearer token
(`anyRequest().fullyAuthenticated()`), so an anonymous call is rejected with `401` before it reaches a controller.
The endpoint then checks a **semantic role** with `@PreAuthorize`, and a token without it is rejected with `403`.

A semantic role resolves to `<system-name>_@<resource>_#<operation>`, where `<system-name>` is
`jeap.security.oauth2.resourceserver.system-name`.

| Resource                   | Operation | Protects                                                                                                                     |
| -------------------------- | --------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `openapidoc`               | `write`   | Pushing an OpenAPI spec, when authenticated with a bearer token                                                              |
| `database-schema`          | `write`   | Pushing a database schema                                                                                                    |
| `external-database-schema` | `read`    | Reading a database schema over `/external-api/dbschemas`                                                                     |
| `architecture-model`       | `read`    | **Every** resource of the [docs API](docs-api.md). There is no public and no merely authenticated resource under `/docs-api` |

So an instance whose `system-name` is `my-system` grants its pushing pipeline
`my-system_@openapidoc_#write` and `my-system_@database-schema_#write`.

### `system-name` is required

Semantic authorization is only active when `jeap.security.oauth2.resourceserver.system-name` is set. Without it
the starter falls back to a simple expression handler that does not know the two-argument `hasRole(resource,
operation)` form at all, and every annotated endpoint fails to evaluate its expression.

Because the [docs API](docs-api.md) authorizes **every** one of its resources with a semantic role, the service
**refuses to start** when the property is missing, rather than leaving an endpoint that fails on first use. Every
instance has to set it.

## Read replicas

The read-only endpoints of the service are annotated `@TransactionalReadReplica` from `jeap-spring-boot-tx`
rather than `@Transactional(readOnly = true)`. On an instance configured with an AWS read replica
(`jeap.datasource.replica.enabled=true`) their transactions are routed there and relieve the writer; without one
the annotation resolves to the ordinary transaction manager, so nothing changes.

Reads from a replica are eventually consistent, which is why only endpoints that tolerate a short staleness window
carry the annotation - and why none of them writes. The [docs API](docs-api.md) is deliberately free of writes for
that reason: the content hash it serves as an entity tag is written when an artifact is stored, and for artifacts
predating the column by the `V2_6_0` migration, never by a request.

## Related

- [Docs API](docs-api.md) - the API that requires the `architecture-model` role
- [Configuration](configuration.md) - where the properties live
- [API](api.md) - which endpoint sits behind which mechanism
- [Getting started](getting-started.md) - the minimal instance configuration
- [Operations](operations.md) - the endpoints the basic credentials are actually used for
