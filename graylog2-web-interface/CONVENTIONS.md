# Frontend Conventions

Coding conventions for the Graylog web interface. They apply to all changes, whether written by humans or AI agents.

## Code Style

- ESLint (`eslint-config-graylog`, based on [Airbnb](https://github.com/airbnb/javascript)) and Stylelint (`stylelint-config-graylog`) are authoritative for code style and patterns.
- Fix visible ESLint warnings in files you touch.

## Naming

- **Functions**: use a verb as the name.
- **Classes**: use a noun as the name.
- **React components**: PascalCase file names (`MyComponent.tsx`).

## Components

- Use **TypeScript** for new components. Migrate existing JS components to TS when touching them.
- Prefer **functional components** with hooks. Class components are acceptable for complex cases, but when in doubt, use functional.
- When touching existing components, migrate them to functional, typed components. Exception: trivial bugfixes where migration effort exceeds the fix or risks unforeseen consequences.
- Keep components under 300 lines.
- Components should not contain business logic. Extract transformation/computation logic into helper functions in nearby files, so it can be reused and tested independently.
- React Compiler handles render optimization. Do not add `useMemo` or `useCallback` only for optimization.

### Reusing Components

- Use styled-components, Mantine, and the local component abstractions.
- Reuse shared components before creating new ones. Check the [frontend documentation](https://graylog2.github.io/frontend-documentation) for available common components.
- Shared components live in `src/components/common`. Wrapped Mantine components live in `src/components/bootstrap`.
- For common UI patterns, prefer existing shared UI components from `src/components/bootstrap` and `src/components/common` over native HTML elements.

## Type Definitions

### Component Props

- **No PropTypes** — support was dropped with React 19. Use TypeScript types for props.
- For the main exported component in a file, name the component props type `Props` and place it directly above the component.
- When typing components with children, prefer `React.PropsWithChildren` over adding a `children` field to the props type directly.

### Type Safety

- Prefer TypeScript's type inference over unnecessary explicit type annotations. Add explicit types when they improve clarity or are required, not by default.
- Avoid redundant function type annotations when the expected type is already inferred from usage, such as callbacks passed to typed component props.
- Avoid explicit function return types when TypeScript can infer them clearly.
- Exception: for exported hooks that wrap `react-query`, prefer explicitly typing the hook's public return shape to the fields our code actually uses instead of exposing the full `useQuery` result type. This makes the contract clearer and keeps tests easier to mock.
- Give unused parameters a meaningful name after the underscore (e.g., `_eventType`), not just `_`. See [this discussion](https://github.com/Graylog2/graylog2-server/pull/12176#pullrequestreview-940555887).
- `types.d.ts` can hide errors like missing imports. Temporarily rename to `types.ts` to detect them.
- Do not leave out types for function arguments (therefore being implicitly `any`), use proper types.
- Do not use `as unknown as <...>` to opt out of type-checking.

## Imports

- Modules with one export should use default export.
- With multiple exports, use default export only if one serves the module's main purpose.
- `index.ts` barrel files in component folders can simplify imports but may introduce cyclic dependencies — use with caution.

## State Management

**New code** should use (in order of simplicity):

1. `useState` — for local component state
2. `useContext` — for state shared across a component hierarchy
3. Redux — for complex state

**No Reflux for new code.** Existing Reflux stores:

- Prefer replacing with `react-query` (API caching) or `useState`/`useContext` (state).
- If migration isn't possible yet, access via `useStore`.

## Common Libraries

- For server state and API communication, use `@tanstack/react-query`, preferably behind dedicated hooks instead of calling `useQuery` or `useMutation` directly in many components.
- Use the typed API stubs from `@graylog/server-api` and `@graylog/enterprise-api`.
- For forms, use `formik` and prefer existing Formik-based helpers and components when they fit.
- For component styling, use `styled-components` and prefer theme tokens over hard-coded values.

## Testing

### General

- **Framework**: Jest + [Testing Library](https://testing-library.com/).
- Test feature behavior and public interfaces from the user's perspective, not internal implementation details.
- One feature test per feature (e.g. an entity data table) covers its components and hooks together. Separate unit tests per file are not the goal.
- Only add a dedicated unit test for a hook or helper if it has logic that a feature test can't reach or verify cleanly.
- Check existing tests first. Adjust tests when behavior changes, extend them for important missing scenarios, and add tests when a changed feature has no useful coverage.
- **No snapshot tests** for component state. Use Testing Library queries (`getByText`, etc.) instead. Snapshot tests are acceptable for verifying complex function return values.

### Render and Wrappers

- Import `render` from `wrappedTestingLibrary`, not directly from `@testing-library/react`.
- Use the default wrappers provided by `wrappedTestingLibrary` and `wrappedTestingLibrary/hooks`. Do not manually add providers that are already included there, such as `DefaultQueryClientProvider`, unless the test needs a specific override or custom setup.

### Assertions and Queries

- Follow Testing Library's [Guiding Principles](https://testing-library.com/docs/guiding-principles) and their guide for [picking a good query](https://testing-library.com/docs/queries/about#priority).
- Prefer `findBy` over `getBy` for UI that may update asynchronously. It matches async UI behavior and avoids brittle manual waiting.
- Prefer `await screen.findBy...` directly over wrapping it in `expect(...).toBeInTheDocument()`. `findBy` already fails if the element is not present.

### Mocking

- When mocking API communication in tests, do not mock `@tanstack/react-query` directly. Mock the abstraction on top of it instead, which is usually a dedicated hook.
- If a `react-query` hook currently lives inside a component and the test needs to mock it, move that hook into a separate file so it can be mocked cleanly.
- Never cast to `jest.Mock` (`foo as jest.Mock`), use the `asMock` helper function.

### Fixtures

- Prefer existing fixtures over creating large inline entity mocks inside a test. Search for reusable fixtures nearby and in shared test fixture locations before creating a new one.
- If no suitable fixture exists, extract the mock data into a fixture file instead of keeping a complex object inline in the test. Follow the local pattern when one exists; otherwise place the fixture near the test, typically in a nearby `__tests__` directory.

### Test File Placement

Test files go next to their source files, with the same name and a `.test.tsx` suffix:

```
ComponentA.tsx
ComponentA.test.tsx
```

If fixtures are needed, use a `__tests__/` directory:

```
ComponentA.tsx
__tests__/ComponentA.test.tsx
__tests__/ComponentA.test.case1.json
```

## JavaScript Gotchas

### Default Values

Use nullish coalescing (`??`) instead of logical OR (`||`) for defaults:

```js
// ?? only replaces undefined/null
const a = undefined ?? 'default'; // 'default'
const b = false ?? 'default'; // false
const c = 0 ?? 'default'; // 0
const d = '' ?? 'default'; // ''

// || replaces all falsy values (usually not what you want)
const e = false || 'default'; // 'default'
const f = 0 || 'default'; // 'default'
```

Default parameters and destructuring only assign defaults when the value is `undefined`, not `null`:

```js
const test = ({ value1 = 12, value2 = 34 }) => console.log(value1, value2);
test({ value1: undefined, value2: null }); // 12, null
```

### Avoid `Array.reduce` for Object Construction

`Array.reduce` is slow for building objects from large arrays. Use `Object.fromEntries` instead. See [this PR](https://github.com/Graylog2/graylog2-server/pull/12162) for details.

### Date and Time

- Avoid using `moment` directly in application code when shared date/time abstractions already cover the use case. Prefer `util/DateTime` and user-facing helpers such as `useUserDateTime`.
- If the required date/time logic is missing from the shared abstraction, extend `util/DateTime` instead of introducing new direct `moment` usage. This keeps future migration away from `moment` easier.

## Session Timeouts

To prevent session expiry during user interaction, every API request using `fetch` from `FetchProvider` extends the session. Periodic requests must use `fetchPeriodically` instead to avoid extending the session when the user is idle. When using the generated stubs, pass the optional last options argument with `{ requestShouldExtendSession: false }`.

## Plugin System

- Register: `PluginStore.register(new PluginManifest({}, { key: [data] }));`
- Consume: `usePluginEntities('key')`
- No central documentation of plugin store keys — search the codebase for usage.
- **Merging bindings:** whenever you combine multiple plugin bindings (`PluginExports` objects) into one — e.g. assembling a `bindings.tsx` from sub-plugins, or aggregating bindings for `PluginStore.register` — you **must** use `mergePluginBindings` from `util/mergePluginBindings`. Do **not** use `lodash/merge`, `Immutable.Map().mergeWith`, object spreads (`{ ...a, ...b }`), or hand-written per-key array concatenation: those overwrite or index-merge array-valued keys (e.g. `pageNavigation`, `routes`) and silently drop entries. `mergePluginBindings` concatenates array-valued keys while deep-merging the rest.

## Browser Compatibility

- Follow the supported browser targets defined in `supportedBrowsers.js`. When making browser-sensitive changes, consider the browsers we currently build assets for and do not add compatibility workarounds for older unsupported browsers.

## UI Styling

The [graylog-luma design system](https://graylog2.github.io/design-system) — especially the **Foundation** and **Patterns** sections — documents conventions (spacing/typography tokens, form layout, entity-creation flows, content and writing rules) that go beyond what's summarized below. Its sources live in `docs/graylog-luma/stories/`; guidance and token values also live in plain `.tsx` companions and `.stories.tsx` component files, not just `.mdx` docs.

### Styled Components

- In styled components, prefer theme tokens over hard-coded style values when possible. This includes spacing via `theme.spacings`, colors via `theme.colors`, and typography values such as `theme.fonts.family` and `theme.fonts.size`.
- To style a component, prefer wrapping that component with `styled(...)` instead of targeting it through selectors from a parent component.

### Responsive Styles

- Large and medium screens: all features must work.
- Mobile: graphs and complex layouts may have limitations.

### Button Colors

| Color  | Variant   | Use for                          |
| ------ | --------- | -------------------------------- |
| Grey   | `default` | Neutral actions                  |
| Blue   | `info`    | Neutral actions                  |
| Red    | `danger`  | Destructive actions              |
| Yellow | `warning` | Potentially dangerous actions    |
| Green  | `success` | Creative actions (creating data) |

Avoid `link` style buttons — use actual anchors for navigation.

### Page Loading

- Use `Spinner` when loading data from the backend.

### Modals

- ESC key must close modals.
- Form modals should not close on outside click (to prevent data loss).
- Form modals should autofocus the first input.

## Refactoring

- Separate refactoring into dedicated commits.
- If refactoring grows large, create a separate PR.
- Near releases or for backported changes, weigh the risk of refactoring — defer if it adds too many changes.
