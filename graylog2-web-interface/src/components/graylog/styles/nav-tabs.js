import { css } from 'styled-components';
import { breakpoint, util } from 'theme';

const navTabsStyles = () => css(({ theme }) => {
  const borderColor = util.colorLevel(theme.color.variant.info, -3);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          &:hover {
            border-color: ${theme.color.gray[90]} ${theme.color.gray[90]} ${borderColor};
            background-color: ${theme.color.gray[90]};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.color.gray[10]};
            background-color: ${theme.color.gray[100]};
            border-color: ${borderColor};
            border-bottom-color: transparent;
          }
        }
      }

      &.nav-justified {
        > .active > a,
        > .active > a:hover,
        > .active > a:focus {
          border-color: ${borderColor};
        }

        @media (min-width: ${breakpoint.min.sm}) {
          > li > a {
            border-bottom-color: ${borderColor};
          }
          > .active > a,
          > .active > a:hover,
          > .active > a:focus {
            border-bottom-color: ${theme.color.gray[100]};
          }
        }
      }
    }
  `;
});

export default navTabsStyles;
