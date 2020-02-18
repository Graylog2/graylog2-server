import { css } from 'styled-components';

import { util } from 'theme';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    > li > a {
      color: ${theme.color.primary.tre};

      :hover,
      :focus {
        color: ${util.contrastingColor(theme.color.secondary.due)};
        background-color: ${theme.color.secondary.due};
      }
    }

    > .active > a {
      background-color: ${theme.color.tertiary.due};
      color: ${util.contrastingColor(theme.color.tertiary.due)};

      :hover,
      :focus {
        background-color: ${theme.color.tertiary.uno};
        color: ${util.contrastingColor(theme.color.tertiary.uno)};
      }
    }

    > .disabled > a {
      color: ${util.contrastingColor(theme.color.primary.tre, 'AA')};

      :hover,
      :focus {
        background-color: ${theme.color.secondary.due};
        color: ${util.contrastingColor(theme.color.primary.tre, 'AAA')};
      }
    }
  }
`);

export default menuItemStyles;
