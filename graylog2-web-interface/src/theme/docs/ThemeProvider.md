`ThemeProvider` is created in `src/theme/GraylogThemeProvider.jsx` and is used in `src/index.jsx` to wrap the entire application.

This will allow developers to access all [`theme`](https://styled-components.com/docs/api#themeprovider) props. The `theme` object contains [`color`](#section-colors) currently, but will soon adapt to the entire layout of the application.

For theme variants, we are using [`styled-theming`](https://github.com/styled-components/styled-theming/tree/v2.2.0) to allow for multiple themes in the future.
