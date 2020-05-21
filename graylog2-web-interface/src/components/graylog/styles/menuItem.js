import { css } from 'styled-components';

import { util } from 'theme';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    > li > a {
      color: ${theme.colors.global.textDefault};

      :hover,
      :focus {
        color: ${util.contrastingColor(theme.colors.gray[90])};
        background-color: ${theme.colors.gray[90]};
      }
    }

    > .active > a {
      color: ${util.contrastingColor(theme.colors.variant.light.info)};
      background-color: ${theme.colors.variant.light.info};

      :hover,
      :focus {
        color: ${util.contrastingColor(theme.colors.variant.info)};
        background-color: ${theme.colors.variant.info};
      }
    }

    > .disabled > a {
      color: ${util.contrastingColor(theme.colors.gray[90], 'AA')};
      background-color: ${theme.colors.gray[90]};

      :hover,
      :focus {
        color: ${util.contrastingColor(theme.colors.gray[90], 'AA')};
      }
    }
  }
`);

export default menuItemStyles;
