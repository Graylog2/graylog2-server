import { css } from 'styled-components';

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    background-color: ${theme.colors.global.contentBackground};
    box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
    
    > li > a {
      color: ${theme.colors.global.textDefault};

      :hover,
      :focus {
        color: ${theme.colors.variant.darker.default};
        background-color: ${theme.colors.variant.lightest.default};
      }
    }

    > .active > a {
      color: ${theme.colors.variant.darker.default};
      background-color: ${theme.colors.variant.lightest.default};

      :hover,
      :focus {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.variant.lighter.default};
      }
    }

    > .disabled > a {
      color: ${theme.colors.variant.dark.default};
      background-color: ${theme.colors.variant.lightest.default};

      :hover,
      :focus {
        color: ${theme.colors.variant.dark.default};
        background-color: ${theme.colors.variant.lightest.default};
      }
    }
  }
`);

export default menuItemStyles;
