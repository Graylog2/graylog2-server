# Agent Instructions

This file contains instructions for AI coding agents working on the Graylog web interface.

**You must also read [CONVENTIONS.md](./CONVENTIONS.md)** before changing code — it contains the coding conventions, component guidelines, testing standards, and UI styling rules that apply to all changes.

## Project Overview

- **Project**: Graylog Web Interface (`graylog-web-interface`)
- **Language**: TypeScript, React
- **Package manager**: Yarn (v1)
- **Bundler**: Webpack
- **Test framework**: Jest + Testing Library
- **Linter**: ESLint (extends `eslint-config-graylog`, based on Airbnb)
- **Style linter**: Stylelint (`stylelint-config-graylog`)

## Commands

```bash
# Install dependencies
yarn install

# Start dev server
yarn start

# Start dev server without plugins
disable_plugins=true yarn start

# Build (without plugins)
yarn build

# Run all tests (DO NOT use `yarn jest`)
yarn test

# Run a specific test
yarn test --testPathPattern=<pattern>

# Type check
yarn tsgo

# Lint changed files (requires committed changes)
yarn lint:changes

# Lint a specific file
yarn lint:path <file>

# Lint styles
yarn lint:styles

# Lint styles for a specific file
yarn lint:styles:path <file>

# Format code
yarn format

# Pre-PR verification (run before considering work complete)
yarn tsgo && yarn lint:changes && yarn test
```

## Project Structure

- `src/` — Application source code (components, views, stores, actions, logic)
- `packages/graylog-web-plugin/` — Shared packages for core and plugins, webpack config for plugins, plugin registration interfaces
- `packages/eslint-config-graylog/` — Custom ESLint rules
- `packages/stylelint-config-graylog/` — Custom Stylelint rules
- `docs/graylog-luma/stories/` — Sources of the graylog-luma design system
- `target/` — Build output

## Design System

When a change touches UI, consult the graylog-luma design system in `docs/graylog-luma/stories/`. Read every file relevant to the change, not just `.mdx` docs.

## Finishing work

Before finishing current work, please make sure that:
- Type-checking passes
- Tests are passing (use `yarn test`, never `yarn jest`)
- Generated code adheres to [CONVENTIONS.md](./CONVENTIONS.md)
