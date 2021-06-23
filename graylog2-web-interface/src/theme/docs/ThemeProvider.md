[`ThemeProvider`](https://styled-components.com/docs/api#themeprovider) is created in `src/theme/GraylogThemeProvider.jsx` and is used in `src/index.jsx` to wrap the entire application.

This will allow developers to access all `theme` props in each component. In most cases, on a new component, you can access the `theme` prop via Styled-Components like the following:

```jsx static
const StyledElement = styled.div(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
`);
```

If you need to base some styles off of props, you can access them as well.

```jsx static
const StyledElement = styled.div<{ wide: boolean }>(({ wide, theme }) => css`
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
```
