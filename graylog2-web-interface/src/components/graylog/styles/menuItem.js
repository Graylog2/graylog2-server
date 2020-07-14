import { css } from 'styled-components';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    > li > a {
      color: ${theme.colors.global.textDefault};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.colors.variant.lighter.default)};
        background-color: ${theme.colors.variant.lighter.default};
      }
    }

    > .active > a {
      color: ${theme.utils.contrastingColor(theme.colors.variant.lighter.default)};
      background-color: ${theme.colors.variant.lighter.default};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.colors.variant.light.default)};
        background-color: ${theme.colors.variant.light.default};
      }
    }

    > .disabled > a {
      color: ${theme.utils.contrastingColor(theme.colors.variant.lighter.default, 'AA')};
      background-color: ${theme.colors.variant.lighter.default};

      :hover,
      :focus {
        color: ${theme.utils.contrastingColor(theme.colors.variant.lighter.default, 'AA')};
        background-color: ${theme.colors.variant.lighter.default};
      }
    }
  }
`);

export default menuItemStyles;
