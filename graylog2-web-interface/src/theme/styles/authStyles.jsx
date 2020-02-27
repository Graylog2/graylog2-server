import { createGlobalStyle } from 'styled-components';

import bgImage from 'images/auth/loginbg.jpg';

const AuthThemeStyles = createGlobalStyle`
  body {
    /* we love science */
    background: url(${bgImage}) no-repeat center center fixed;
    background-size: cover;
  }
`;

export default AuthThemeStyles;
