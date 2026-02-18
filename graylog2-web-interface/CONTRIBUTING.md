# Contributing

Thank you for contributing to the Graylog web interface. This guide covers conventions and standards for both human contributors and AI coding agents.

For general contribution instructions, visit [graylog.org/get-involved](https://www.graylog.org/get-involved/).

> **AI agents**: Also read [AGENT.md](./AGENT.md) for agent-specific instructions, commands, and project structure details.

## Code of Conduct

In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making participation in our project and our community a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

Please read and understand the [Code of Conduct](https://github.com/Graylog2/graylog2-server/blob/master/CODE_OF_CONDUCT.md).

## Code Style

- We use ESLint to detect issues in our code, mostly following the [Airbnb Javascript style guide](https://github.com/airbnb/javascript) with some exceptions.
- Custom rules are maintained in [`eslint-config-graylog`](https://raw.githubusercontent.com/Graylog2/graylog2-server/master/graylog2-web-interface/packages/eslint-config-graylog/index.js).
- Enable linter hints in your IDE and consider enabling "fix on save" ([IntelliJ docs](https://www.jetbrains.com/help/idea/eslint.html#ws_eslint_configure_run_eslint_on_save)).
- A CI job checks for linter hints in changed files.
- `yarn lint:changes` — lint all changed files (requires committed changes).
- `yarn lint:path <file>` — lint a specific file.

## Naming

- **Functions**: use a verb as the name.
- **Classes**: use a noun as the name.

## Components

### Class vs Functional

Small components should be functional. For complex components, either class or functional with hooks is acceptable — when in doubt, prefer functional.

### Size and Simplicity

- Keep components under 300 lines.
- Components should not contain business logic. Extract transformation/computation logic into helper functions that can be reused and tested independently.

### Reusing Components

- We wrap react-bootstrap components in our own wrappers, importable from `components/graylog`. Always use these wrappers instead of importing react-bootstrap directly.
- Check the [frontend documentation](https://graylog2.github.io/frontend-documentation) for available common components before creating new ones.

## Type Definitions

- Use **TypeScript** for all new React components with static types for props.
- **No PropTypes** — support was dropped with React 19.
- Use **default parameters** instead of `defaultProps` in functional components. See the [React 19 upgrade guide](https://react.dev/blog/2024/04/25/react-19-upgrade-guide).
- When touching existing components, migrate them to functional, typed components. Exception: trivial bugfixes where migration effort exceeds the fix or risks unforeseen consequences.
- Prefix unused parameters with an underscore and a meaningful name (e.g., `_eventType`), not just `_`. See [this discussion](https://github.com/Graylog2/graylog2-server/pull/12176#pullrequestreview-940555887).
- `types.d.ts` can hide errors like missing imports. Temporarily rename to `types.ts` to detect them.

## Imports

- Prefer ES6 modules (`import`/`export`) over CommonJS `require`.
- Modules with one export should use default export.
- With multiple exports, use default export only if one serves the module's main purpose.
- `index.ts` barrel files in component folders can simplify imports but may introduce cyclic dependencies — use with caution.

## State Management

**New code** should use (in order of simplicity):

1. `useState` — for local component state
2. `useContext` — for state shared across a component hierarchy
3. Redux — for complex state

**Existing Reflux stores** (discouraged for new code):

- Prefer replacing with `react-query` (API caching) or `useState`/`useContext` (state).
- If migration isn't possible yet, access via `useStore`.

## Testing

- **Framework**: Jest + [Testing Library](https://testing-library.com/).
- Follow Testing Library's [Guiding Principles](https://testing-library.com/docs/guiding-principles) and their guide for [picking a good query](https://testing-library.com/docs/queries/about#priority).
- Import `render` from `wrappedTestingLibrary`, not directly from `@testing-library/react`.
- Write unit tests for every use case of new functionality.
- Test from the user's perspective — do not rely on internal implementation details.
- **No snapshot tests** for component state. Use Testing Library queries (`getByText`, etc.) instead. Snapshot tests are acceptable for verifying complex function return values.

### Test File Placement

Test files go next to their source files:

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

### Test Timeouts

Some tests are flaky under resource constraints ("Exceeded timeout of 5000 ms"). Adjust workers:

- `yarn test --maxWorkers=150%` — increase timeout margin.
- `yarn test --maxWorkers=25%` — simulate low-power environments.

### Useful Tool

The Chrome extension "Testing Playground" helps find the best queries to select elements.

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

## Session Timeouts

To prevent session expiry during user interaction, every API request using `fetch` from `FetchProvider` extends the session. Periodic requests must use `fetchPeriodically` instead to avoid extending the session when the user is idle.

## Plugin System

- Register: `PluginStore.register(new PluginManifest({}, { key: [data] }));`
- Consume: `usePluginEntities('key')`
- No central docs for plugin store keys — search the codebase.
- Test without plugins: `disable_plugins=true yarn start`
- Example plugin: [graylog-plugin-sample](https://github.com/Graylog2/graylog-plugin-sample)

## Internal Packages

- `graylog-web-plugin` — shared packages for core and plugins, webpack config for plugin builds, plugin registration interfaces.
- `eslint-config-graylog` — custom ESLint config based on eslint-config-airbnb.
- `stylelint-config-graylog` — custom Stylelint config.

## Refactoring

- Fix visible ESLint warnings in files you touch.
- Separate refactoring into dedicated commits.
- If refactoring grows large, create a separate PR.
- Near releases or for backported changes, weigh the risk of refactoring — defer if it adds too many changes.

## Working on a Feature

Test thoroughly before submitting a PR:

- Different user roles (admin, reader, minimal permissions).
- Different screen resolutions.
- Different browsers (especially Safari).
- Heterogeneous data and high data volumes.

Run checks locally before creating a PR:

```sh
yarn tsc && yarn lint:changes && yarn test
```

## Browser Compatibility

Test layout changes in Chrome, Firefox, and Safari. Larger layout changes should also be tested in older browsers. See the [browser compatibility list](https://docs.graylog.org/docs/web-interface#browser-compatibility).

## UI Styling

### Forms

- Use vertically aligned labels and inputs (no horizontal forms without good reason).
- Add helper text to inputs.
- Show validation state only after changes or form submission.
- Prefer our own `Select` component over the native one.
- Avoid long forms in modals.
- Mark optional fields with "(Optional)" in the label.

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

## Common Problems

### Yarn Cache

The yarn cache can grow very large (200GB+). Run `yarn cache clean` periodically.
