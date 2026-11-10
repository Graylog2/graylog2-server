# Contributing

Thank you for contributing to the Graylog web interface.

For general contribution instructions, visit [graylog.org/get-involved](https://www.graylog.org/get-involved/).

- **Coding conventions**: see [CONVENTIONS.md](./CONVENTIONS.md). They apply to all changes.
- **Commands** (dev server, tests, linting, type checking) and **project structure**: see [AGENTS.md](./AGENTS.md#commands).

## Code of Conduct

In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making participation in our project and our community a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

Please read and understand the [Code of Conduct](https://github.com/Graylog2/graylog2-server/blob/master/CODE_OF_CONDUCT.md).

## Editor Setup

- Enable linter hints in your IDE and consider enabling "fix on save" ([IntelliJ docs](https://www.jetbrains.com/help/idea/eslint.html#ws_eslint_configure_run_eslint_on_save)).
- A CI job checks for linter hints in changed files.

## Working on a Feature

Test thoroughly before submitting a PR:

- Different user roles (admin, reader, minimal permissions).
- Different screen resolutions.
- Different browsers (especially Safari). Test layout changes in Chrome, Firefox, and Safari. Larger layout changes should also be tested in older browsers. See the [browser compatibility list](https://docs.graylog.org/docs/web-interface#browser-compatibility).
- Heterogeneous data and high data volumes.
- Without plugins: `disable_plugins=true yarn start`.

Run checks locally before creating a PR:

```sh
yarn tsgo && yarn lint:changes && yarn test
```

## Useful Tools and Resources

- The Chrome extension "Testing Playground" helps find the best queries to select elements.
- Example plugin: [graylog-plugin-sample](https://github.com/Graylog2/graylog-plugin-sample).
