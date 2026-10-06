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

- Reuse shared components before creating new ones or using native HTML elements. See the [frontend documentation](https://graylog2.github.io/frontend-documentation).
- `src/components/common`: shared components. `src/components/bootstrap`: our Mantine and react-bootstrap wrappers.

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
- API calls: typed stubs from `@graylog/server-api` and `@graylog/enterprise-api`.
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
- Use `asMock(foo)`, never `foo as jest.Mock`.
- Reuse existing fixtures instead of large inline mocks. If none fits, create a fixture file following the local pattern, otherwise in a nearby `__tests__/` directory.
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

## Refactoring

- Put refactoring in separate commits, and in a separate PR if it grows large.
- Near releases or for backports, defer refactoring that adds too much risk.
