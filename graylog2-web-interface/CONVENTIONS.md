# Frontend Conventions

Coding conventions for the Graylog web interface. They apply to all changes, whether written by humans or AI agents.

## Code Style

- ESLint (`eslint-config-graylog`, based on [Airbnb](https://github.com/airbnb/javascript)) and Stylelint (`stylelint-config-graylog`) are authoritative.
- Fix visible ESLint warnings in files you touch.

## Naming

- Functions: verbs. Classes: nouns.
- React components: PascalCase file names (`MyComponent.tsx`).

## Components

- New components: TypeScript, functional, with hooks. Class components only for complex cases.
- When touching existing components, migrate them to typed functional components — except for trivial bugfixes where migration is riskier than the fix.
- Keep components under 300 lines.
- No business logic in components. Extract it into helper functions in nearby files, so it can be reused and tested.
- Don't add `useMemo`/`useCallback` only for performance — React Compiler handles it.

### Reusing Components

- Reuse shared components before creating new ones or using native HTML elements. Look at the [graylog-luma design system](https://graylog2.github.io/design-system) first (sources in `docs/graylog-luma/stories/`), then `src/components/common` and `src/components/bootstrap`.
- `src/components/common`: shared components, including thin wrappers around Mantine components (`Stack`, `Text`, `Tooltip`, …). New shared components go here.
- `src/components/bootstrap`: replacements for former react-bootstrap components that keep their API (e.g. `Button` with `bsStyle`), today mostly built on Mantine. Only add a component here when it replaces a react-bootstrap one.
- The goal is a consistent UI the designer has approved, so avoid one-off UI in feature code. If a shared component is missing, create it in `src/components/common`, even while the design is still open, and document it with a story in `docs/graylog-luma/stories/`. Without an approved design, say so in the story and the PR, and open a follow-up issue to align it with the designer.
- Patterns, recurring combinations of components such as entity list pages (`PageHeader` with a `CreateButton`, then a `PaginatedEntityTable`), follow `docs/graylog-luma/stories/Patterns/` or, where a pattern isn't documented yet, the composition of existing pages of the same kind. Document a missing pattern in a separate PR. Check the Figma file or ask the designer first; if there is no design yet, start from what the code does and get the PR reviewed by frontend and design.

## Types

- Use TypeScript types for props, no PropTypes (dropped in React 19).
- Name the props type of a file's main component `Props` and place it directly above the component.
- For children, use `React.PropsWithChildren` instead of a `children` prop.
- Rely on type inference. Only add explicit types (including return types and callback types) when required or clearer.
  - Exception: exported `react-query` hooks explicitly type their return value to the fields we use, not the full `useQuery` result. This keeps the contract clear and tests easy to mock.
- Type all function arguments, no implicit `any`.
- Don't use `as unknown as <...>` to bypass type-checking.
- Unused parameters: `_` plus a meaningful name (`_eventType`, not `_`). See [discussion](https://github.com/Graylog2/graylog2-server/pull/12176#pullrequestreview-940555887).
- `types.d.ts` can hide errors like missing imports — temporarily rename it to `types.ts` to reveal them.

## Imports

- Single export: default export. Multiple exports: default export only for the module's main purpose.
- Use `index.ts` barrel files with caution — they can cause cyclic dependencies.

## State and Data

- State, in order of preference: `useState` (local) → `useContext` (shared in a hierarchy) → Redux (complex).
- No Reflux in new code. Replace existing stores with `react-query` or `useState`/`useContext` where possible; otherwise access them via `useStore`.
- Server state: `@tanstack/react-query`, wrapped in dedicated hooks instead of calling `useQuery`/`useMutation` in many components.
- API calls: typed stubs from `@graylog/server-api` and `@graylog/enterprise-api`. They are generated into `target/api` from the Swagger definition of the Maven build (`yarn generate:apidefs`) and not committed. Don't edit them. If a stub is still missing after regenerating, the backend API has to change first.
- Feature flags: read them with `useFeature('flag_name')`. Flags are defined and enabled by the backend, the frontend can't add one alone.
- Forms: `formik`, reusing existing Formik-based helpers and components.
- Session: every `fetch` from `FetchProvider` extends the user's session. Periodic requests must not — use `fetchPeriodically`, or pass `{ requestShouldExtendSession: false }` as the last argument of generated stubs.

## Testing

- Jest + [Testing Library](https://testing-library.com/). Test behavior from the user's perspective, not implementation details.
- One feature test per feature (e.g. an entity data table) covering its components and hooks. Add a separate unit test only for logic the feature test can't reach.
- Check existing tests first. Adjust them when behavior changes; add tests for important missing scenarios and for changed features without useful coverage.
- No snapshot tests for component state — use queries like `getByText`. Snapshots are fine for complex function return values.
- Import `render` from `wrappedTestingLibrary` (or `wrappedTestingLibrary/hooks`). Don't re-add providers it already includes (e.g. `DefaultQueryClientProvider`) unless you need an override.
- Choose queries by Testing Library's [priority guide](https://testing-library.com/docs/queries/about#priority).
- Use `findBy` for UI that may update asynchronously, and `await screen.findBy…` on its own — it already fails if the element is missing, no `expect(…).toBeInTheDocument()` needed.
- Don't mock `@tanstack/react-query`; mock the hook on top of it. If that hook lives inside a component, move it to its own file.
- Use `asMock(foo)`, never `foo as jest.Mock`. Mock Reflux stores with `MockStore` (both from `helpers/mocking`).
- For `react-select` inputs, use `helpers/selectEvent`.
- `console.warn` and `console.error` fail tests. Fix the cause; wrap only expected errors in `helpers/suppressConsole`.
- Reuse existing fixtures (shared ones in `test/fixtures/`, imported as `fixtures/...`) instead of large inline mocks. If none fits, create a fixture file following the local pattern, otherwise in a nearby `__tests__/` directory.
- Placement: `ComponentA.test.tsx` next to `ComponentA.tsx`. With fixtures: `__tests__/ComponentA.test.tsx` and `__tests__/ComponentA.test.case1.json`.

## JavaScript

- Use `??` instead of `||` for defaults. `||` also replaces `0`, `''` and `false`.
- Default parameters and destructuring defaults only apply to `undefined`, not `null`.
- Build objects with `Object.fromEntries`, not `Array.reduce`, which is slow for large arrays ([details](https://github.com/Graylog2/graylog2-server/pull/12162)).
- Dates: use `util/DateTime` and helpers like `useUserDateTime`, not `moment` directly. Extend `util/DateTime` if something is missing, to ease a future migration away from `moment`.

## Plugin System

- Register: `PluginStore.register(new PluginManifest({}, { key: [data] }));`. Consume: `usePluginEntities('key')`.
- Plugin store keys are not documented centrally — search the codebase.
- To combine plugin bindings (`PluginExports` objects), you **must** use `mergePluginBindings` from `util/mergePluginBindings`. `lodash/merge`, `Immutable.Map().mergeWith`, object spreads or manual concatenation overwrite or index-merge array-valued keys (e.g. `pageNavigation`, `routes`) and silently drop entries.

## Browser Compatibility

- Target the browsers in `supportedBrowsers.js`. Don't add workarounds for older, unsupported browsers.

## UI Styling

The [graylog-luma design system](https://graylog2.github.io/design-system) (sources in `docs/graylog-luma/stories/`) documents further conventions, especially in **Foundation** and **Patterns**: tokens, form layout, entity-creation flows, writing rules. Guidance also lives in `.tsx` and `.stories.tsx` files, not just `.mdx`.

- Use `styled-components` with theme tokens instead of hard-coded values: `theme.spacings`, `theme.colors`, `theme.fonts.family`, `theme.fonts.size`.
- Style a component by wrapping it with `styled(...)`, not via selectors from a parent.
- All features must work on large and medium screens. On mobile, graphs and complex layouts may be limited.
- Use `Spinner` while loading data from the backend.
- Modals close on ESC. Form modals don't close on outside click (prevents data loss) and autofocus the first input.
- Button variants:

| Variant   | Color  | Use for                          |
| --------- | ------ | -------------------------------- |
| `default` | Grey   | Neutral actions                  |
| `info`    | Blue   | Neutral actions                  |
| `danger`  | Red    | Destructive actions              |
| `warning` | Yellow | Potentially dangerous actions    |
| `success` | Green  | Creative actions (creating data) |

- No `link` style buttons — use anchors for navigation.

## Theme

The theme lives in `src/theme/sawmill`. `THEME_BASE.ts` defines the color shade matrix, which the Mantine theme is built on. `styled-components/utils/colors.ts` maps these shades to the tokens of the styled-components theme (`theme.colors.*`), which match the tokens in Figma.

- Only our designer changes the color shade matrix in `THEME_BASE.ts`. To change the color of an element or token, point the token to a different shade instead.
- Reuse existing tokens where they fit. Tokens named after their purpose, like `theme.colors.input.borderFocus`, are only used for that purpose.
- Prefer descriptive tokens like `theme.colors.section.filled.background` over generic helpers like `theme.colors.variant.lighter.danger` or `theme.colors.gray[60]`. Descriptive tokens can use different shades in light and dark mode.
- New tokens need a name that describes their usage. Inform the designer when you add one. Add them to `styled-components/utils/colors.ts` and their type to `styled-components/types.ts`. Applying an opacity with `opacify` is fine.
- The default themes are read from the generated `mantine/generated/theme.json` and `styled-components/generated/theme.json`. After changing `THEME_BASE.ts` or the color utils, run `yarn generate:themes` and commit the generated files. Custom themes from the customization page are generated at runtime.

## Translations

User-visible texts are translated with [i18next](https://www.i18next.com/) and `react-i18next`.

- Texts live in `src/locales/<language>/<namespace>.json`, one namespace per area (`common` for shared components, `search`, `streams`, …). Register new namespaces in `src/locales/registerCoreTranslations.ts`; plugins call `registerTranslations` from `i18n` with their own namespaces.
- Keys are nested, `snake_case` and describe the purpose, not the English text: `t('bulk_actions.cancel')`.
- Translate whole sentences. Use interpolation (`{{name}}`) for values and `<Trans>` for markup, never concatenate translated fragments. Word order and grammar differ between languages.
- `<Trans>` fills its components with the text between the tags, so pass components which render `children` (wrap components like `DocumentationLink` that take a `text` prop). Don't name tags like HTML void elements (`link`, `input`, `img`, …), their content gets lost.
- Use plurals via `count` (`items_one`, `items_other`) instead of `StringUtils.pluralize`.
- Translate at render time: call `t()` inside components, also for default prop values. Module-level constants hold keys, not texts. Outside of components, call `i18n.t()` from `i18n` when the text is needed.
- Never translate user content (e.g. stream or dashboard titles) or technical values (field names, query syntax). Messages of thrown errors and console output are for developers and stay English.
- German translations follow `src/locales/GLOSSARY.de.md`.
- Navigation items (`navigation`, `pageNavigation`, `helpMenu` plugin exports) keep their English `description`, because it identifies them, e.g. to merge items of plugins or for `position.after`. Set `titleKey` (e.g. `navigation:main.search`) to translate the displayed title.
- Format numbers, dates and durations with `util/NumberFormatting`, `util/DateTime` (e.g. `preciseDifference`) and `useUserDateTime`. They follow the UI language.
- English texts are the fallback. Add every new key to all languages in `src/locales`; `locales.test.ts` fails otherwise. Untranslated drafts may be added in English and marked for review.
- Tests fail when a component uses a key which does not exist. Keys are not type-checked, because i18next's key types require `strictNullChecks`.
- Directories which are completely translated enable the `graylog/no-untranslated-text` lint rule in `eslint.config.mjs`.
- In development mode, the "Pseudo" language shows translated texts with accents and extra length. Text without accents is not translated yet.

## Refactoring

- Put refactoring in separate commits, and in a separate PR if it grows large.
- Near releases or for backports, defer refactoring that adds too much risk.
