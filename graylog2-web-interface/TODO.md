# TODO: Translate the Graylog web interface

Working notes for continuing the i18n effort on branch `feature/i18n-search-page` (graylog2-server).
Not meant to be committed.

## 1. Where we are

Done and committed (each commit is self-contained, types/lint/tests green):

| Area | Namespace(s) |
| --- | --- |
| Infrastructure: shared i18next singleton, vendor bundle, lazy languages, `LanguageProvider`, localized durations/numbers, Jest setup, missing-key guard, locale consistency test, lint rule, pseudo language, docs | — |
| Base components (`src/components/common`, `bootstrap`, `navigation`), language picker in preferences/user menu | `common`, `navigation` |
| Login page | `login` |
| Welcome page, onboarding, content stream | `welcome` |
| All of `src/views` (search page, dashboards view, widgets, message table, export, aggregation wizard, visualizations, dashboards overview, field/value actions) | `search`, `widgets`, `aggregation`, `dashboards` |

Last commit: `6f40bce588 i18n(search): Translate view logic, field and value actions`.

Read first: `CONVENTIONS.md` (section "Translations") and `src/locales/GLOSSARY.de.md`.

## 2. Partially translated areas (WIP commits)

Several areas were started but not finished. They are committed as `WIP i18n(<area>): …` commits (authentication,
cluster certificates, collectors, content packs, data node, outputs, events, event definitions, event
notifications, inputs, extractors). The keys they use now exist in `src/locales/{en,de}`, and all namespaces are
registered, but the areas still contain untranslated text. Finish them like any other area (see section 4): run
the lint rule on the area, translate what is left, add the area to `TRANSLATED_FILES` and drop `WIP` when the
commits are later turned into PRs.

```bash
python3 dev/i18n/find-missing-keys.py src     # must report 0 missing keys
```

The script does not see keys built at runtime (template literals, key maps, local `t` wrappers around `i18n.t`).
Check those by hand; the Jest missing-key handler catches them for rendered components.

To **restart** an area instead of finishing it, revert its WIP commit (`git revert <commit>`, or drop it in an
interactive rebase done by a human) and do the area from scratch.

**Every new namespace must be registered** in `src/locales/registerCoreTranslations.ts` (alphabetical, English
import + lazy German chunk). An unregistered namespace renders raw keys in the browser, and the Jest missing-key
handler only catches it for components that tests actually render.

Optional clean-up: authentication and collectors use one namespace per page (`authentication_page`,
`authentication_create`, …, `collectors_fleets`, …). Merging them into one namespace per area (`authentication`,
`collectors`) with one section per page matches the convention used elsewhere and is preferred, but not required.

Never use `git stash` here: the repo has older stashes of the user (`stash@{0..2}`) that must stay untouched.

## 3. Remaining areas (one commit per area, in this order)

Sizes are untranslated JSX texts found by the lint rule; notifications/validation strings in `.ts` files come on top.

| # | Area | Directories / pages (under `src`) | Namespace | Size |
| --- | --- | --- | --- | --- |
| 1 | Streams & outputs | `components/streams`, `components/streamrules`, `components/outputs`, `pages/Stream*.tsx`, `pages/SystemOutputsPage.tsx` | `streams` | ~250 |
| 2 | Alerts/events | `components/events`, `pages/EventsPage.tsx` | `events` (sections `alerts_*`) | part of ~650 |
| 3 | Event definitions | `components/event-definitions`, `pages/*EventDefinition*.tsx`, `pages/useEventDefinitionDetailSections.tsx` | `events` (sections `definitions_*`) | ~350 |
| 4 | Event notifications | `components/event-notifications`, `integrations/event-notifications`, `pages/*EventNotification*.tsx` | `events` (sections `notifications_*`) | part of ~650 |
| 5 | Inputs | `components/inputs`, `components/messageloaders`, `pages/{Inputs,NodeInputs,InputDiagnosis}Page.tsx` | `inputs` | part of ~500 |
| 6 | Extractors | `components/extractors`, `pages/*Extractors*.tsx`, display names in `util/ExtractorUtils.ts` | `extractors` | part of ~500 |
| 7 | System | `components/{nodes,loggers,metrics,notifications,systemjobs,systemmessages,indexers,cluster,configurations,cluster-configuration,configurationforms}`, `pages/{SystemOverview,ShowNode,ShowMetrics,Loggers,SystemLogs,IndexerFailures,SystemNotifications,ProcessBufferDump,Configurations,ClusterConfiguration*}Page.tsx` | `system` | large, split |
| 8 | Data Node | `components/datanode`, `pages/DataNode*Page.tsx` | `datanode` | ~70 files |
| 9 | Indices | `components/indices`, `pages/Indices*Page.tsx`, `pages/IndexSet*Page.tsx` | `indices` | ~160 files |
| 10 | Users, teams, roles, permissions | `components/{users,roles,permissions}`, user/role pages | `users` | ~115 files |
| 11 | Authentication | `components/authentication`, `pages/Authentication*Page.tsx` | `authentication` | ~65 files |
| 12 | Pipelines, rules, simulator, grok | `components/{pipelines,rules,simulator,grok-patterns}`, related pages | `pipelines` | ~70 files |
| 13 | Lookup tables | `components/lookup-tables`, `components/maps`, `pages/LUT*Page.tsx` | `lookup_tables` | ~95 files |
| 14 | Sidecars | `components/sidecars`, `pages/Sidecar*Page.tsx` | `sidecars` | ~50 files |
| 15 | Collectors | `components/collectors`, `pages/Collectors*Page.tsx` | `collectors` | ~105 files |
| 16 | Content packs | `components/content-packs`, `pages/*ContentPack*Page.tsx` | `content_packs` | ~33 files |
| 17 | Rest of core | `components/{quick-jump,hotkeys,security,enterprise,upsell,errors,layout,throughput,…}`, `integrations/**` (rest), `threatintel`, `aws`, error pages (`NotFoundPage`, `RuntimeErrorPage`, `ServerUnavailablePage`), `KeyboardShortcutsPage`, `ApiBrowserPage` | existing or `system` | final sweep |
| 18 | Preflight | `src/preflight/**` (separate webpack entry `webpack.preflight.ts`): needs its own i18n init and browser language detection | `preflight` | ~47 files |
| 19 | Enterprise plugin | `graylog-plugin-enterprise/enterprise/src/web/**` (~2,500 files): register `enterprise_<area>` namespaces via `registerTranslations` from `i18n` in the plugin entry, navigation via `titleKey` (`EnterpriseMenuBindings.tsx`) | `enterprise_<area>` | huge, per area |

Find remaining work anywhere:
```bash
node_modules/.bin/eslint --rule '{"graylog/no-untranslated-text":"error"}' <dirs or files>
```

## 4. Workflow per area

Environment (shell does not load nvm/sdkman):
```bash
export PATH=$HOME/.nvm/versions/node/v22.22.3/bin:$PATH
cd graylog2-web-interface
```

1. List texts: run the lint rule above on the area; also `grep -rn -E "UserNotification\.|defaultOnError\(" <dirs>` for
   notifications, and look for validation messages, option label constants and `bindings.ts` titles.
2. Translate the code (see rules in section 5). Put keys into `src/locales/en/<ns>.json` and `src/locales/de/<ns>.json`
   (nested, snake_case, same structure).
3. New namespace: register it in `src/locales/registerCoreTranslations.ts` (alphabetical, English import + lazy German
   chunk, copy an existing entry).
4. Checks:
   ```bash
   node_modules/.bin/oxfmt <changed files>
   yarn -s lint:path --fix <changed files>
   yarn -s tsgo
   yarn -s lint:path --rule '{"graylog/no-untranslated-text":"error"}' <changed files>
   python3 dev/i18n/find-missing-keys.py
   yarn -s test --testPathPatterns='(<area dirs>|locales/locales)'
   yarn -s test        # full run, ~11 min
   ```
   Full runs on this machine have flaky 5 s timeouts; rerun failing suites alone
   (`yarn -s test --testPathPatterns='<Suite>'`) before assuming a real failure.
   Pre-existing lint errors (React Compiler rules `set-state-in-effect`, `refs during render`, `cannot create
   components`, tanstack `exhaustive-deps` on queryKey, some jsx-a11y) exist on master already; compare with
   `git show HEAD:<path> | node_modules/.bin/eslint --stdin --stdin-filename <path>` and leave them.
5. Add the area's directories to `TRANSLATED_FILES` in `eslint.config.mjs` once they are free of untranslated text.
6. Commit only that area's files:
   `git add <files>` then `git commit` with message `i18n(<namespace>): Translate <area>` + short body (why/how, bugs
   fixed) + trailer `Co-Authored-By: …`. Don't push, no PRs (they are created later from the commits).
   Intermittent `fatal: detected dubious ownership` from git: check `git log -1` / `git status` before retrying.

Note: `node_modules/jest-preset-graylog` and `node_modules/eslint-plugin-graylog` are *copies* of `packages/*`. When
changing those packages, copy the changed files into `node_modules` (or run `yarn install`) before running tests/lint.

## 5. Rules and pitfalls (learned so far)

- English texts stay word-for-word identical (tests assert them), except real bugs. German: formal "Sie", follow the
  glossary, feature names (Stream, Dashboard, Widget, Input, Pipeline, Sidecar, …) stay English. German is marked
  Beta until reviewed by a native speaker.
- Whole sentences with interpolation (`{{name}}`); never concatenate translated fragments. Markup via `<Trans>`.
- `<Trans>` components must render `children`. Components with a `text` prop (e.g. `DocumentationLink`) or required
  children (`ExternalLink`) need a tiny wrapper: `const DocsLink = ({ children = undefined }) => <DocumentationLink page={…} text={children} />`.
- Never name `<Trans>` tags like HTML void elements (`link`, `input`, `img`, `br`, …): their content gets lost.
  `locales.test.ts` rejects them. Use e.g. `configurationsLink`.
- Plurals via `count` (`key_one`/`key_other`) instead of `StringUtils.pluralize`. German may need contexts (e.g.
  `duration.days_ago_other` for the dative after "vor").
- No lower-casing in code (German nouns are capitalized): use `{{x, lowercase}}` / `{{x, capitalize}}` formats.
- Translate at render time: default props become `prop = undefined` + `prop ?? t('…')`; module-level constants become
  key maps; plain functions/hooks use `i18n.t(key, { ns })` (`import i18n from 'i18n'`). Class components: small
  function components for translated parts, or `i18n.t` in render; class `defaultProps` get explicit `undefined`.
- Identifiers stay English: telemetry values, React keys, ids derived from labels (`AccordionItem` derives its
  eventKey from `name` → pass `id`), values compared in code, stored data (default view titles, chart axis titles),
  thrown errors and console output, query syntax, keyword time ranges (server parses English only), field names.
- Plugin-extensible labels (navigation, page navigation, help menu, field/value actions, widget creators, aggregation
  elements, export strategies) keep the English `title`/`description` as identifier and use an optional `titleKey`
  (namespaced, e.g. `navigation:main.search`) for display; render sites fall back to the plain text.
- `PaginatedEntityTable`: pass a translated `humanName`.
- `Select` only uses an explicit `placeholder` as `aria-label`; don't pass translated defaults as aria-label.
- Server-generated texts (notification templates, API validation errors, entity attribute titles from the server)
  are out of scope.
- Keys are not type-checked (`strict: false` breaks i18next key types); the Jest missing-key handler and
  `dev/i18n/find-missing-keys.py` are the guards.

## 6. Open follow-ups (not blocking)

- Events widget column titles (`views.components.widgets.events.attributes` plugin binding) and message preview
  options (`messageTable.previewOptions`) need a `titleKey`-like mechanism.
- Quick jump: titles of configuration pages and actions (`components/quick-jump/hooks/useNavItems.ts`).
- Root admin can't store a language (server returns fixed preferences, `UserImpl.java`).
- `QueryHelper` (`components/common/QueryHelper.tsx`) field descriptions.
- After all areas: run the lint rule over all of `src`, then replace the `TRANSLATED_FILES` list by `src/**`.
- Native-speaker review of German, then set `beta: false` for German in `src/components/users/languages/Languages.ts`.
- Delete this file and `dev/i18n/find-missing-keys.py` (or keep the script in `dev/`) before creating PRs.
