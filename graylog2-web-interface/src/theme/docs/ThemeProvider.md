[`ThemeProvider`](https://styled-components.com/docs/api#themeprovider) is created in `src/theme/GraylogThemeProvider.jsx` and is used in `src/index.jsx` to wrap the entire application.

This will allow developers to access all `theme` props in each component. In most cases, on a new component, you can access the `theme` prop via Styled-Components like the following:

```jsx static
const StyledElement = styled.div(({ theme }) => `
  background-color: ${theme.colors.global.contentBackground};
`);
```

Or, if you are using Flow, you can type the component with

```jsx static
import styled, { type StyledComponent } from 'styled-components';
import { type ThemeInterface } from 'theme/types';

const StyledElement: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  background-color: ${theme.colors.global.contentBackground};
`);
```

If you need to base some styles off of props, you can access them as well.

```jsx static
const StyledElement = styled.div(({ wide, theme }) => `
  background-color: ${theme.colors.global.contentBackground};
  width: ${wide ? '100%' : '50%'};
`);
// or
const StyledElement: StyledComponent<{wide: boolean}, ThemeInterface, HTMLDivElement> = styled.div(({ wide, theme }) => `
  background-color: ${theme.colors.global.contentBackground};
  width: ${wide ? '100%' : '50%'};
`);

// usage
<StyledElement wide>
```

If you do not need the theme colors in your component

```jsx static
const StyledElement = styled.div`
  opacity: 0.5;
`;
// or
const StyledElement: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  opacity: 0.5;
`;
```
