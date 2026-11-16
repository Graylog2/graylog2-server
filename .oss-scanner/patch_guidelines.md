# How we would like patches made

For code style and tests, follow `AGENTS.md` at the repository root, and for frontend changes also
`graylog2-web-interface/AGENTS.md` and `graylog2-web-interface/CONVENTIONS.md`. The rest of this file covers what
is specific to the patches you send us.

## Size and scope of a fix

- Fix the root cause, as narrowly as possible. No refactoring, renaming or reformatting of code you do not need
  to touch.
- If the same mistake is repeated in sibling code (for example the same missing permission check on several
  methods of one REST resource), fix all of them in the same patch and say so.
- Prefer existing mechanisms over new ones: `@RequiresPermissions` and `RestPermissions` constants for REST
  authorization, `EntityPermissionsUtils` and the grants/shares services for entity access, the URL allowlist
  service for outbound URLs, `AllowedAuxiliaryPathChecker` for file paths.
- Do not change public REST API paths or response formats unless the fix needs it; if it does, say why.

## Tests

- Add a regression test that fails without the fix and passes with it, in the existing test class for the
  changed class when there is one.
- Prefer plain unit tests over tests that need MongoDB, OpenSearch or Testcontainers; those cannot run in the
  scanner image.
- Make sure the module still compiles and its existing tests for the changed classes pass.

## Branch, commit message, comments and changelog

The patch reaches us with the report, and we commit it ourselves when we are ready. That commit is public before
the advisory is. Its diff shows what was fixed, which cannot be avoided: still make the complete fix, with its
regression test. What must not point to the vulnerability is how the change is described, in the commit message,
code comments and test names. Describe what the code now does, not the attack it prevents, and name tests after
the behavior they check (for example `rejectsRuleUpdateWithoutStreamEditPermission`).

- Against the `master` branch.
- Commit subject: an imperative sentence describing the change, without a component prefix and without saying it
  is a security fix (for example `Check stream permission when updating stream rules`).
- Code comments only where the code is not obvious, and without referring to the report or the scanner.
- Do not add a file under `changelog/`; we add the entry when we publish the advisory.
