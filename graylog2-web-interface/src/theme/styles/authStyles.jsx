import { createGlobalStyle, css } from 'styled-components';

import bgImage from 'images/auth/loginbg.jpg';

const AuthThemeStyles = createGlobalStyle(({ theme }) => css`
  body {
    /* we love science */
    background: url(${bgImage}) no-repeat center center fixed ${theme.color.global.background};
    background-size: cover;
  }
`);

export default AuthThemeStyles;
