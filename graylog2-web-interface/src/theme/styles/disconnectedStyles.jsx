import { createGlobalStyle, css } from 'styled-components';
import bgImage from 'images/auth/loginbg.jpg';

const DisconnectedThemeStyles = createGlobalStyle(({ theme }) => css`
  body {
    /* we love science */
    background: url(${bgImage}) no-repeat center center fixed ${theme.color.secondary.due};
    background-size: cover;
  }
`);

export default DisconnectedThemeStyles;
