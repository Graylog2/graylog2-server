# How we would like patches made

Follow `AGENTS.md` at the repository root, and `graylog2-web-interface/AGENTS.md` plus
`graylog2-web-interface/CONVENTIONS.md` for frontend changes. The points below matter most.

## Size and scope of a fix

- Fix the root cause, as narrowly as possible. No refactoring, renaming or reformatting of code you do not need
  to touch.
- If the same mistake is repeated in sibling code (for example the same missing permission check on several
  methods of one REST resource), fix all of them in the same patch and say so.
- Prefer existing mechanisms over new ones: `@RequiresPermissions` and `RestPermissions` constants for REST
  authorization, `EntityPermissionsUtils` and the grants/shares services for entity access, the URL allowlist
  service for outbound URLs, `AllowedAuxiliaryPathChecker` for file paths.
- Do not change public REST API paths or response formats unless the fix needs it; if it does, say why.
- Do not upgrade dependencies as part of a fix.

## Code style

- Java 21. Follow the Java code style in `AGENTS.md`: no wildcard imports, `final` for local variables, the
  documented import order, `f()` from `org.graylog2.shared.utilities.StringUtils` for formatted strings and
  SLF4J `{}` placeholders for logging.
- New Java files need the SSPL license header used by the other files in `src/main/java` and `src/test/java`.
- The build enforces forbidden APIs (see the `forbiddenapis` configuration in `pom.xml`); for example, always pass
  a charset.
- Comments only where the code is not obvious. Do not reference the report or the scanner in code comments.
- Frontend: TypeScript and React, following `CONVENTIONS.md`; never render message data as HTML.

## Tests

- Add a regression test that fails without the fix and passes with it, in the existing test class for the
  changed class when there is one. Java: JUnit 5, Mockito and AssertJ. Frontend: Jest and Testing Library, run
  with `yarn test <path>`.
- Prefer plain unit tests over tests that need MongoDB, OpenSearch or Testcontainers; those cannot run in the
  scanner image.
- Make sure the module still compiles and its existing tests for the changed classes pass.

## Branch, commit message and changelog

- Against the `master` branch.
- Commit subject: an imperative sentence describing the change, without a component prefix and without saying it
  is a security fix (for example `Check stream permission when updating stream rules`).
- Do not add a file under `changelog/`; we add the entry when we publish the advisory.
