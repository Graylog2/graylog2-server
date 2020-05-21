import { css } from 'styled-components';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    > li > a {
      color: ${theme.color.global.textDefault};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.color.gray[90])};
        background-color: ${theme.color.gray[90]};
      }
    }

    > .active > a {
      color: ${theme.utils.contrastingColor(theme.color.variant.light.info)};
      background-color: ${theme.color.variant.light.info};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.color.variant.info)};
        background-color: ${theme.color.variant.info};
      }
    }

    > .disabled > a {
      color: ${theme.utils.contrastingColor(theme.color.gray[90], 'AA')};
      background-color: ${theme.color.gray[90]};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.color.gray[90], 'AA')};
      }
    }
  }
`);

export default menuItemStyles;
