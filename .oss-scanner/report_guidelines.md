# How we would like reports written

We turn confirmed reports into GitHub Security Advisories, so a report that already has the facts an advisory
needs saves us the most time.

## Layout and length

One root cause per report. Keep the prose under one page, not counting the reproduction and the advisory text. Use
these sections, in this order:

1. **Title**: `<Component>: <weakness> in <class or endpoint>`, for example
   `REST API: missing permission check in StreamResource.update`.
2. **Summary**: two or three sentences: what an attacker can do, from which starting position, and the impact.
3. **Classification**:
   - Product: `Graylog Server` or `Graylog Data Node` (exactly one of these names).
   - CWE: the most specific CWE that matches the root cause (for example CWE-639 rather than CWE-284,
     CWE-770 rather than CWE-400).
   - CVSS v3.1 vector and base score. Optionally a CVSS v4.0 vector on the next line.
4. **Affected versions and configuration**: the first affected version if you can tell from the git history,
   otherwise "at least since <commit or version>". State every non-default setting the issue needs.
5. **Attacker's starting position**: unauthenticated network access to which port or input, or an account
   with which role and which permissions.
6. **Root cause**: file paths with line numbers, and the code path from the entry point (REST resource, input,
   pipeline function) to the faulty code.
7. **Reproduction**: a unit test where possible (JUnit for the server and Data Node, Jest with `yarn test` for the
   web interface), otherwise a script that runs offline in the scanner image or against a Graylog server, with
   the command that runs it. Show the output you observed, if you could run it, and say what output would be
   correct.
8. **Suggested fix**: one paragraph; the patch itself goes in the patch.
9. **Advisory text**: a draft of the public advisory, as described below.

## Advisory text

When we release the fix, we publish a GitHub Security Advisory. Draft its text for us. Its readers are Graylog
administrators deciding whether they are affected and what to do, so it is much shorter than the report and must
not help anyone exploit the issue: no class names, file paths, code paths, request payloads or reproduction steps.
Name features, permissions, inputs and settings the way administrators know them.

Put the draft in one fenced `markdown` block, with the title on the first line and then these sections:

- **Title**: one line naming the affected feature and the impact, for example
  `Privilege escalation via user creation and user permission/role updates`.
- `### Impact`: what an attacker can do, from which starting position, and which installations are affected
  (features in use, roles that hold a permission, non-default settings).
- `### Patches`: `Fixed in Graylog <versions>.`, leaving the versions to us, then one sentence on how the fix
  changes the behavior.
- `### Workarounds`: concrete steps for administrators who cannot upgrade right away, such as restricting a
  permission or limiting network access to an input, or `There is no known workaround.`

Leave out credits; we add them.

## What every report should state

- Whether you ran the reproduction and saw it succeed, or reasoned about it from the code only.
- The REST endpoint (method and path) or the input type and port that reaches the faulty code.
- What data or capability the attacker gains, in concrete terms (which messages, which entities, which files).

## Style

- Plain, factual English. No marketing language, no speculation about attacker motives.
- Markdown with fenced code blocks; no tables.
- Do not include working exploit code beyond what is needed to show the bug, and no payloads that harm a real
  system (no reverse shells, no data exfiltration to external hosts).
