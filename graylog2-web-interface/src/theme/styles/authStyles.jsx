import { css } from 'styled-components';

import bgImage from 'images/auth/loginbg.jpg';

const authStyles = css(({ theme }) => css`
  body {
    /* we love science */
    background: url(${bgImage}) no-repeat center center fixed ${theme.colors.global.background};
    background-size: cover;
  }
`);

export default authStyles;
