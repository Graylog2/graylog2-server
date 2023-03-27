import { createGlobalStyle, css } from 'styled-components';

const GlobalThemeStyles = createGlobalStyle(({ theme }) => css`
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html {
  font-size: ${theme.fonts.size.root.px};
}
  
body {
  background-color: ${theme.colors.global.background};
  color: ${theme.colors.global.textDefault};
  font-family: ${theme.fonts.family.body};
  height: 100vh;
}
p {
  margin: inherit;
}
  
#app-root {
  height: 100%;
}
  
ul {
  list-style-type: none;
  margin: 0;
}
ul.no-padding {
  padding: 0;
}
hr {
  border-top: 1px solid ${theme.colors.global.background};
}
h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: normal;
  padding: 0;
  margin: 0;
  color: ${theme.colors.global.textDefault};
}
h1 {
  font-size: ${theme.fonts.size.h1.rem};
}
h2 {
  font-size: ${theme.fonts.size.h2.rem};
}
h3 {
  font-size: ${theme.fonts.size.h3.rem};
}
h4 {
  font-size: ${theme.fonts.size.h4.rem};
}
h5 {
  font-size: ${theme.fonts.size.h5.rem};
}
h6 {
  font-size: ${theme.fonts.size.h6.rem};
  font-weight: bold;
}
a {
  color: ${theme.colors.global.link};
}
a:hover,
a:focus {
  color: ${theme.colors.global.linkHover};
}
  
.content {
  padding: ${theme.spacings.md};
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.gray[80]};
  margin-bottom: ${theme.spacings.sm};
  p.description {
    margin-top: 3px;
    color: ${theme.colors.gray[50]};
  }
}
`);

export default GlobalThemeStyles;
