import { createGlobalStyle } from 'styled-components';

const AuthThemeStyles = createGlobalStyle`
  body {
    /* we love science */
    background: url('images/auth/loginbg.jpg') no-repeat center center fixed;
    background-size: cover;
  }
`;

export default AuthThemeStyles;
