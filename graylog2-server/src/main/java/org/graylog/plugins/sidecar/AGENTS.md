# Sidecar

This package is the server side of the [Graylog Sidecar](https://github.com/Graylog2/collector-sidecar), a separate Go process that runs on customer hosts and manages log collectors. Sidecars are updated independently of the server, so a server talks to sidecars of older versions.

## The contract

The sidecar calls these endpoints (see `api/graylog.go` in collector-sidecar):

| Call | Server code |
|---|---|
| `PUT /sidecars/{nodeId}` (registration and heartbeat) | `rest/resources/SidecarResource.java` `register()` |
| `GET /sidecar/collectors` | `rest/resources/CollectorResource.java` |
| `GET /sidecar/configurations/render/{nodeId}/{configurationId}` | `rest/resources/ConfigurationResource.java` |
| `GET /` (server info) | not in this package |

- Request and response JSON: `rest/requests/RegistrationRequest.java`, `rest/models/NodeDetails.java`, `rest/responses/RegistrationResponse.java`, `rest/models/SidecarRegistrationConfiguration.java`, `rest/models/Collector.java`.
- Headers: the sidecar sends `X-Graylog-Sidecar-Version` (required) and `If-None-Match`. The server answers `304 Not Modified` when the ETag matches, cached in `services/EtagService.java`.
- Collector configuration templates get `sidecarVersion` in their context (`services/ConfigurationService.java`).

## Before changing it

- Don't rename, remove or change the type of a JSON field or header above. Add new fields as optional; older sidecars ignore them, and `NodeDetails` ignores unknown fields from newer sidecars.
- When a response changes, check that ETag invalidation still covers it (`services/EtagCacheInvalidation.java`), or sidecars keep the old version.
- A change on the sidecar side belongs in collector-sidecar and needs its own release.
- Registration is a frequent ping and is not audited on purpose (`@NoAuditEvent` on `register()`).
- Tests live in `src/test/java/org/graylog/plugins/sidecar/`.
