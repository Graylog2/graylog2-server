# Prettier → Oxfmt Migration

**Date:** 2026-04-27
**Scope:** `graylog2-server/graylog2-web-interface` and `graylog-plugin-enterprise/enterprise`

## Goal

Replace prettier with [oxfmt](https://oxc.rs/docs/guide/usage/formatter.html) (the OXC project's formatter) across both frontend repos. Oxfmt is ~30× faster than prettier and supports the same key formatting options.

## Affected Repositories

| Repo | Path |
|------|------|
| graylog2-server | `graylog2-web-interface/` |
| graylog-plugin-enterprise | `enterprise/` |

## Changes

### 1. Both repos (identical steps)

**Dependencies:**
- Add `oxfmt` to `devDependencies`
- Remove `prettier` from devDependencies (enterprise: remove from `dependencies`)
- Remove `@graylog/prettier-config` from devDependencies (enterprise: from `dependencies`)

**Config:**
- Run `oxfmt --migrate=prettier` to generate `.oxfmtrc.json` from the existing `.prettierrc.json`
- Verify generated config matches current settings: `singleQuote: true`, `printWidth: 120`, `bracketSameLine: true`, `quoteProps: "preserve"`
- Delete `.prettierrc.json`

**Scripts (`package.json`):**
- `graylog2-web-interface`: `"format": "oxfmt src test docs"`
- `enterprise`: `"format": "oxfmt src/web"`

**CI:** No changes — GitHub Actions workflow calls `yarn format`, which continues to work.

### 2. `packages/eslint-config-graylog` (graylog2-server only)

- Remove `eslint-config-prettier` from `dependencies` in `packages/eslint-config-graylog/package.json`
- No replacement needed (it was not imported or used in `index.js`)

## Out of Scope

- The published `@graylog/prettier-config` npm package — leave as-is, just stop referencing it
- ESLint plugin integration (`eslint-plugin-oxfmt`) — deferred
- Any oxfmt config options beyond what prettier was already enforcing
