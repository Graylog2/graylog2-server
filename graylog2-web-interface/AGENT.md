# Agent Instructions

This file contains instructions for AI coding agents working on the Graylog web interface.

**You must also read [CONTRIBUTING.md](./CONTRIBUTING.md)** — it contains coding conventions, component guidelines, testing standards, and UI styling rules that apply to all changes.

## Project Overview

- **Project**: Graylog Web Interface (`graylog-web-interface`)
- **Language**: TypeScript, React
- **Package manager**: Yarn (v1)
- **Bundler**: Webpack
- **Test framework**: Jest + Testing Library
- **Linter**: ESLint (extends `eslint-config-graylog`, based on Airbnb)
- **Style linter**: Stylelint (`stylelint-config-graylog`)

## Common Commands

| Task                   | Command                                          |
| ---------------------- | ------------------------------------------------ |
| Install dependencies   | `yarn install`                                   |
| Start dev server       | `yarn start`                                     |
| Start without plugins  | `disable_plugins=true yarn start`                |
| Build (no plugins)     | `yarn build`                                     |
| Run all tests          | `yarn test`                                      |
| Run a specific test    | `yarn test --testPathPattern=<pattern>`          |
| Type check             | `yarn tsc`                                       |
| Lint changed files     | `yarn lint:changes` (requires committed changes) |
| Lint a specific file   | `yarn lint:path <file>`                          |
| Lint styles            | `yarn lint:styles`                               |
| Lint styles for a file | `yarn lint:styles:path <file>`                   |
| Format code            | `yarn format`                                    |

## Pre-PR Verification

Before considering work complete, run:

```sh
yarn tsc && yarn lint:changes && yarn test
```

If tests time out, try `yarn test --maxWorkers=150%`.

## Project Structure

- `src/` — Application source code (components, views, stores, actions, logic)
- `packages/graylog-web-plugin/` — Shared packages for core and plugins, webpack config for plugins
- `packages/eslint-config-graylog/` — Custom ESLint rules
- `packages/stylelint-config-graylog/` — Custom Stylelint rules
- `target/` — Build output

## Key Technical Decisions

- **TypeScript only** for new components. Migrate existing JS components to TS when touching them (except trivial bugfixes).
- **Functional components** preferred. Use hooks (`useState`, `useContext`) for state. Class components are acceptable for complex cases but functional is the default.
- **No PropTypes** — use TypeScript types for props. Use default parameters instead of `defaultProps`.
- **No Reflux for new code** — use `react-query` for API caching, `useState`/`useContext` for state, or redux for complex state. Existing Reflux stores should be accessed via `useStore` if not yet migrated.
- **No snapshot tests** for component state — use Testing Library queries (`getByText`, etc.) instead.
- **ES6 modules** — use `import`/`export`, not `require`.
- **Nullish coalescing (`??`)** over logical OR (`||`) for default values.
- **`Object.fromEntries`** over `Array.reduce` for constructing objects from arrays (performance).
- Wrapper components from `components/graylog` instead of direct react-bootstrap imports.
- Check the [frontend documentation](https://graylog2.github.io/frontend-documentation) for available common components before creating new ones.

## Testing Guidelines

- Import `render` from `wrappedTestingLibrary`, not directly from `@testing-library/react`.
- Place test files next to source files: `Component.tsx` / `Component.test.tsx`.
- If fixtures are needed, use a `__tests__/` directory alongside the component.
- Test from the user's perspective — avoid testing internal implementation details.
- Write tests for every use case of new functionality.

## File Naming and Placement

- React components: PascalCase (`MyComponent.tsx`)
- Test files: same name with `.test.tsx` suffix
- Helper/utility functions: extract into nearby files, not inline in components
- Keep components under 300 lines

## Plugin System

- Register: `PluginStore.register(new PluginManifest({}, { key: [data] }));`
- Consume: `usePluginEntities('key')`
- No central documentation of plugin store keys — search the codebase for usage.
