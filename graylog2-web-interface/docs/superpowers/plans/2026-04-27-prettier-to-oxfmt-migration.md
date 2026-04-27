# Prettier → Oxfmt Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace prettier with oxfmt in `graylog2-web-interface` and `graylog-plugin-enterprise`, and remove the unused `eslint-config-prettier` dep from the shared ESLint config package.

**Architecture:** Each repo gets `oxfmt` added as a devDependency and a `.oxfmtrc.json` config file that encodes the existing prettier settings. The `format` script in each `package.json` is updated to call oxfmt. The `packages/eslint-config-graylog` package has its dead `eslint-config-prettier` dep removed. No functional code changes — this is a tooling swap.

**Tech Stack:** oxfmt (npm package `oxfmt`), Yarn v1

---

### Task 1: Set up oxfmt in `graylog2-web-interface`

**Files:**

- Modify: `graylog2-web-interface/package.json`
- Create: `graylog2-web-interface/.oxfmtrc.json`
- Delete: `graylog2-web-interface/.prettierrc.json`

> Background: The existing `.prettierrc.json` contains only the string `"@graylog/prettier-config"`, which resolves to these settings from the npm package: `singleQuote: true`, `printWidth: 120`, `bracketSameLine: true`, `quoteProps: "preserve"`.

- [ ] **Step 1: Install oxfmt and remove prettier deps**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server/graylog2-web-interface
yarn add -D oxfmt
yarn remove prettier @graylog/prettier-config
```

Expected: `package.json` and `yarn.lock` updated, no errors.

- [ ] **Step 2: Create `.oxfmtrc.json`**

Create `graylog2-web-interface/.oxfmtrc.json` with:

```json
{
  "bracketSameLine": true,
  "quoteProps": "preserve",
  "singleQuote": true,
  "printWidth": 120
}
```

- [ ] **Step 3: Delete `.prettierrc.json`**

```bash
rm /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server/graylog2-web-interface/.prettierrc.json
```

- [ ] **Step 4: Update `format` script in `package.json`**

In `graylog2-web-interface/package.json`, change the `format` script from:

```json
"format": "prettier --write src test docs"
```

to:

```json
"format": "oxfmt src test docs"
```

- [ ] **Step 5: Run `yarn format` to verify oxfmt works**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server/graylog2-web-interface
yarn format
```

Expected: oxfmt runs over `src/`, `test/`, `docs/` and exits 0. Some files may be reformatted on this first run — review the diff with `git diff` and confirm the changes are formatting-only (whitespace, quotes, etc.), not semantic. This is expected.

- [ ] **Step 6: Run type-check to confirm no regressions**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server/graylog2-web-interface
yarn tsc
```

Expected: exits 0 with no errors.

- [ ] **Step 7: Commit**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server
git add graylog2-web-interface/package.json graylog2-web-interface/yarn.lock graylog2-web-interface/.oxfmtrc.json graylog2-web-interface/.prettierrc.json
git add -u graylog2-web-interface/src graylog2-web-interface/test graylog2-web-interface/docs
git commit -m "chore(web): replace prettier with oxfmt"
```

Note: `git add -u` stages all tracked file modifications inside those directories (the reformatted files). The `-u` flag only stages changes to already-tracked files, not new files.

---

### Task 2: Set up oxfmt in `graylog-plugin-enterprise`

**Files:**

- Modify: `enterprise/package.json`
- Create: `enterprise/.oxfmtrc.json`
- Delete: `enterprise/.prettierrc.json`

> Background: In the enterprise repo, `prettier` and `@graylog/prettier-config` are listed under `dependencies` (not `devDependencies`) — `yarn remove` handles this regardless.

- [ ] **Step 1: Install oxfmt and remove prettier deps**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog-plugin-enterprise/enterprise
yarn add -D oxfmt
yarn remove prettier @graylog/prettier-config
```

Expected: `package.json` and `yarn.lock` updated, no errors.

- [ ] **Step 2: Create `.oxfmtrc.json`**

Create `enterprise/.oxfmtrc.json` with:

```json
{
  "bracketSameLine": true,
  "quoteProps": "preserve",
  "singleQuote": true,
  "printWidth": 120
}
```

- [ ] **Step 3: Delete `.prettierrc.json`**

```bash
rm /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog-plugin-enterprise/enterprise/.prettierrc.json
```

- [ ] **Step 4: Update `format` script in `package.json`**

In `enterprise/package.json`, change the `format` script from:

```json
"format": "prettier --write src/web"
```

to:

```json
"format": "oxfmt src/web"
```

- [ ] **Step 5: Run `yarn format` to verify oxfmt works**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog-plugin-enterprise/enterprise
yarn format
```

Expected: exits 0. Some files may be reformatted — review with `git diff` to confirm changes are formatting-only.

- [ ] **Step 6: Run type-check**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog-plugin-enterprise/enterprise
yarn tsc
```

Expected: exits 0.

- [ ] **Step 7: Commit**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog-plugin-enterprise/enterprise
git add package.json yarn.lock .oxfmtrc.json .prettierrc.json
git add -u src/web
git commit -m "chore(enterprise): replace prettier with oxfmt"
```

---

### Task 3: Remove unused `eslint-config-prettier` from `packages/eslint-config-graylog`

**Files:**

- Modify: `graylog2-web-interface/packages/eslint-config-graylog/package.json`

> Background: `eslint-config-prettier` appears in `packages/eslint-config-graylog/package.json` at version `10.1.5` but is never imported in `index.js`. Removing it is a dead-dep cleanup with no functional effect.

- [ ] **Step 1: Remove `eslint-config-prettier` from `package.json`**

Edit `graylog2-web-interface/packages/eslint-config-graylog/package.json` and delete the line:

```json
"eslint-config-prettier": "10.1.5",
```

Then update `yarn.lock`:

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server/graylog2-web-interface
yarn install
```

Expected: `yarn.lock` updated, no errors.

- [ ] **Step 2: Verify ESLint still works**

Run ESLint against a source file to confirm nothing broke:

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server/graylog2-web-interface
yarn lint:path src/views/components/App.tsx
```

If that file doesn't exist, pick any `.tsx` file from `src/`. Expected: lint runs and exits 0 (or with the same pre-existing warnings as before — no new errors).

- [ ] **Step 3: Commit**

```bash
cd /home/dennis/work/graylog-project-repos-vibeenv-infra/graylog2-server
git add graylog2-web-interface/packages/eslint-config-graylog/package.json graylog2-web-interface/yarn.lock
git commit -m "chore(eslint): remove unused eslint-config-prettier dependency"
```
