import { css } from 'styled-components';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    > li > a {
      color: ${theme.colors.global.textDefault};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.colors.gray[90])};
        background-color: ${theme.colors.gray[90]};
      }
    }

    > .active > a {
      color: ${theme.utils.contrastingColor(theme.colors.variant.light.info)};
      background-color: ${theme.colors.variant.light.info};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.colors.variant.info)};
        background-color: ${theme.colors.variant.info};
      }
    }

    > .disabled > a {
      color: ${theme.utils.contrastingColor(theme.colors.gray[90], 'AA')};
      background-color: ${theme.colors.gray[90]};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.colors.gray[90], 'AA')};
      }
    }
  }
`);

export default menuItemStyles;
