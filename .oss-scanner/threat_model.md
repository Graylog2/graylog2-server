# Graylog threat model

This file states our security goals, priorities and severity ratings. The areas and classes it names are
priorities and starting points, not a complete list: look beyond them, and judge any finding against the goals.

## What this project does

Graylog is a log management and SIEM platform. This repository is Graylog Open: the Java server
(`graylog2-server/`), its React web interface (`graylog2-web-interface/`), the Data Node that runs OpenSearch for
it (`data-node/`), and the storage adapters (`graylog-storage-*`). Graylog Enterprise, Forwarder and Sidecar live
in other repositories and are out of scope.

A typical installation is one or more server nodes sharing MongoDB (configuration) and OpenSearch (messages), run
by a security or operations team. Untrusted input enters through:

- **The web interface and REST API** (port 9000, API under `/api`): the boundary between users of different
  privilege.
- **Inputs**: listeners that receive log data (Syslog, GELF, Beats, CEF, NetFlow/IPFIX, OpenTelemetry, the
  collector endpoint and others), often reachable by every logging host and sometimes by the Internet, plus inputs
  that poll or consume external systems.
- **The Data Node API** (port 8999), secured with certificates and JWTs the server issues.
- **Files and packages users upload**: content packs, lookup data, GeoIP databases, certificates.

## Who the attackers are

In order of how much we care:

1. An unauthenticated network attacker who can reach an input, the web interface/API, or a Data Node.
2. Anyone who can get text into a log message. Message content is attacker-controlled by design and flows through
   parsers, pipeline rules and lookups into storage and analysts' screens. Treat every field as hostile.
3. An authenticated user with few permissions (for example the Reader role, or access to only some streams).
4. A user with powerful but non-admin permissions (inputs, pipeline rules, lookup tables, notifications).
5. Whoever supplies a content pack or other uploaded file.

Users with the Admin role are trusted to configure the system, except for the boundaries listed below.

## What must hold

- **Permissions hold between users.** Every API endpoint, MCP tool and search path enforces the caller's
  permissions on the entity it acts on. A user who may read only some streams must not read messages from others
  by any route (search, aggregation, export, dashboards, event definitions, the API directly). Responses must not
  contain entities or fields the caller may not see.
- **No one gains more than they were given.** Creating users, changing roles and permissions, sharing entities
  (view, manage and own are levels) and creating or revoking tokens must never give anyone, the caller or others,
  more than the caller holds. Installing a content pack must not create anything the installing user could not
  create directly.
- **Sessions and tokens end when they should**: after logout, expiry, revocation, a password change, or when the
  user is disabled or deleted. A session ID an attacker plants must not become an authenticated session, and
  trusted-header authentication must accept the header only from `trusted_proxies`.
- **Log content stays data.** Message content must not execute in the browser, must not make a parser overwrite or
  remove other fields or set fields Graylog reserves for itself, and must not make processing stall: a small
  crafted input must not cause unbounded memory, CPU (including regular expressions), decompression or recursion.
- **Proxied API calls cannot be redirected.** Some API endpoints pass a user's request on to another Graylog node
  or a Data Node. The parts the user controls (node ID, path, parameters) must not let them call a different
  endpoint or perform an action their own permissions do not allow. The server's own requests to OpenSearch and
  other backends are not covered by this; what users can see through them is governed by the permission rules
  above.
- **TLS and keys are handled properly.** Outbound TLS verifies certificates and hostnames when configured to;
  passwords and tokens are hashed properly; secrets (`password_secret`, encryption keys, the internal CA's private
  keys, encrypted input and adapter credentials) are never exposed or reused unsafely.
- **The Data Node** accepts only valid JWTs and certificates from its server, and its preflight (setup) mode must
  not be usable by others. The OpenSearch it wraps is part of it: how the Data Node configures, secures and starts
  that OpenSearch (security plugin, TLS, authentication, network binding, file permissions, users and roles) is
  in scope, and that OpenSearch must not be reachable or usable without the credentials the Data Node sets up.

## Past vulnerability classes

Reports we have had fall into these classes; treat them as the most likely places for new ones:

1. Missing or wrong permission checks (CWE-862, CWE-863, CWE-639)
2. Privilege escalation through grants (CWE-269)
3. SSRF (CWE-918)
4. Unbounded resource use in inputs and parsers (CWE-770, CWE-409, CWE-674, CWE-1333)
5. Unsafe file, proxy and class sinks (CWE-22, CWE-470)
6. Data over-exposure in responses (CWE-213, CWE-204)
7. Crypto, keys and TLS validation (CWE-297, CWE-916, CWE-320)
8. Session and token lifecycle (CWE-613, CWE-614)

## Boundaries that hold even for admins

These are vulnerabilities even when the attacker has Admin:

- Executing operating-system commands or arbitrary Java code on the server or Data Node through a feature not
  designed to run code, including by having the server load or instantiate a class not on its allowlist.
- Reading or writing files outside `allowed_auxiliary_paths`, when that setting is configured (it is empty,
  meaning unrestricted, by default), and any path built from request data escaping its directory.
- Reading secrets the API deliberately never returns: stored passwords, encrypted credentials, `password_secret`,
  other users' tokens, the internal CA's private keys.

## Scope

- Less important: metrics, telemetry, version checks, sample-message inputs, Elasticsearch 7 storage (legacy).
- Out of scope: `graylog-plugin-archetype/`, `misc/`, `docs/`, `changelog/`, `api-specs/`, `full-backend-tests/`,
  and test code, except test helpers shipped in a release.
- Out of scope: vulnerabilities in third-party dependencies, unless you show a path through Graylog code that
  reaches them with attacker-controlled input. Version bumps alone are not findings.
- Out of scope for now: the URL allowlist for outgoing HTTP requests. Do not report ways around it.
- Out of scope: MongoDB, the standalone OpenSearch in `/opt/opensearch`, and their configuration in this image.
  They are here only so `graylog-services` can run a server to test against. What Graylog does with them stays in
  scope: the queries it builds from user input and the stream filters it adds.
- In scope: the OpenSearch the Data Node wraps, as the Data Node configures and runs it (see "The Data Node"
  above). Bugs in OpenSearch's own code are third-party issues, unless the Data Node's configuration exposes them.

## How to exercise it

The image contains the full build of `/src`; there is no network.

- **A real server.** Run `graylog-services`. It starts MongoDB, OpenSearch and the Graylog server built from this
  checkout and prints when it is ready (up to five minutes on first start): `http://127.0.0.1:9000`, API under
  `/api`, user `admin`, password `admin`, log in `/var/log/graylog/server.log`. Non-GET API requests need the
  header `X-Requested-By: <anything>`. Create low-privilege users, roles, streams and inputs through the API to
  reproduce authorisation bugs; inputs you start listen on localhost (for example a GELF HTTP input on 12201:
  `curl -XPOST 127.0.0.1:12201/gelf -d '{"short_message":"x","host":"h"}'`).
- **Java unit tests**, offline. Run `graylog-test-hosts` once first (it adds `/etc/hosts` entries some tests need).
  Then: `setpriv --bounding-set=-dac_override,-dac_read_search -- ./mvnw -o test -pl graylog2-server
  -Dskip.web.build=true -Dspotbugs.skip -Dsurefire.excludesFile=/opt/graylog-tests-needing-docker.txt
  -Dtest=<TestClass>` (or `-pl data-node`). The `setpriv` prefix makes root obey file permissions, which some tests
  check. Tests that start containers need Docker, which is not available here; they are listed in
  `/opt/graylog-tests-needing-docker.txt`, so reproduce those cases against `graylog-services` instead. Results of
  the run made at build time: `/var/log/graylog-unit-tests.log` and `graylog2-server/target/surefire-reports/`.
- **Frontend tests**: `cd graylog2-web-interface && export PATH="$PWD/node:$PWD/node/yarn/dist/bin:$PATH" && yarn
  test <path>` (use `yarn test`, not `yarn jest`). The tests run in Jest with jsdom.
- **No browser.** The image has no Chrome or other browser: the only build step that would use one is a
  release check for CI, and installing a browser for it would not help the audit. To confirm XSS, write a Jest
  test that renders the affected component with the hostile data (for example a message field value) and checks
  whether the markup or script ends up in the DOM unescaped, and show with `graylog-services` and the API how
  that data gets stored.

## How we rate severity

We publish advisories with a CVSS v3.1 base score, and expect your severity to match that score's band
(low 0.1-3.9, medium 4.0-6.9, high 7.0-8.9, critical 9.0-10.0). Score the default configuration; a finding that
needs a non-default setting is scored for the configuration that has it, and says so. As guidance:

- **Critical**: remote code execution, or a full authentication bypass, reachable without credentials (from an
  input, the web interface/API, or a Data Node port). Stored XSS or another browser exploit that an
  unauthenticated attacker plants through log data and that runs without unusual user interaction in a
  privileged user's session, leading to account takeover.
- **High**: code execution or escalation to Admin from a low-privilege account. Reading messages from streams the
  user may not see. Stored XSS through log data in an ordinary analyst workflow (search results, message details,
  dashboards). Crashing a server node or Data Node, or stopping message processing for all inputs, with a small
  amount of unauthenticated traffic. Bypassing `allowed_auxiliary_paths`. Reading stored secrets.
- **Medium**: IDOR that reads or changes another user's non-message entities (dashboards, saved searches, event
  definitions, user metadata). Resource exhaustion from attacker-controlled log content that degrades but does not
  stop processing. Stored XSS that needs elevated permissions to plant.
- **Low**: disclosure of non-secret data (internal hostnames, versions, stack traces), CSRF on low-impact actions,
  open redirects, missing hardening.

## Anything to leave alone

- Unauthenticated inputs accepting messages from anyone is by design; input authentication and TLS are optional
  and off by default. Spoofed or forged log messages are not a finding unless they cross a boundary above (for
  example, reaching a stream the sender should not reach when input authentication is enabled, or making a parser
  overwrite or remove other fields).
- Admin-only features doing what they are documented to do, for example a CSV lookup adapter reading a file when
  `allowed_auxiliary_paths` is not configured.
- Attacks that require write access to MongoDB, OpenSearch, the server's configuration file or its filesystem.
- Missing rate limiting on login alone, missing security headers alone, self-XSS, clickjacking without a concrete
  state-changing action, and TLS cipher configuration.
- Denial of service that needs sustained high-volume traffic (a flood) rather than a small crafted input.
