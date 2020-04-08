import { css } from 'styled-components';

import { util } from 'theme';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    > li > a {
      color: ${theme.color.global.textDefault};

      :hover,
      :focus {
        color: ${util.contrastingColor({ color: theme.color.gray[90] })};
        background-color: ${theme.color.gray[90]};
      }
    }

    > .active > a {
      color: ${util.contrastingColor({ color: theme.color.variant.light.info })};
      background-color: ${theme.color.variant.light.info};

      :hover,
      :focus {
        color: ${util.contrastingColor({ color: theme.color.variant.info })};
        background-color: ${theme.color.variant.info};
      }
    }

    > .disabled > a {
      color: ${util.contrastingColor({ color: theme.color.gray[90], level: 'AA' })};
      background-color: ${theme.color.gray[90]};

      :hover,
      :focus {
        color: ${util.contrastingColor({ color: theme.color.gray[90], level: 'AA' })};
      }
    }
  }
`);

export default menuItemStyles;
