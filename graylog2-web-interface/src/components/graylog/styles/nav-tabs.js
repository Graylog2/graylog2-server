import { css } from 'styled-components';
import { breakpoint, util } from 'theme';

const navTabsStyles = css(({ theme }) => {
  const borderColor = util.colorLevel(theme.colors.variant.info, -5);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          transition: background-color 150ms ease-in-out;
          color: ${theme.colors.global.textDefault};
          border-color: ${theme.colors.gray[80]} ${theme.colors.gray[80]} ${borderColor};

          &:hover {
            background-color: ${theme.colors.gray[80]};
            color: ${util.contrastingColor(theme.colors.gray[80])};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.colors.gray[10]};
            background-color: ${theme.colors.gray[100]};
            border-color: ${borderColor};
            border-bottom-color: transparent;
          }
        }

        &.disabled > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.colors.gray[60]};
            background-color: ${theme.colors.gray[100]};
            border-color: ${theme.colors.gray[100]} ${theme.colors.gray[100]} ${borderColor};
            cursor: not-allowed;
          }
        }
      }
    }

    &.nav-justified {
      > .active > a {
        &,
        &:hover,
        &:focus {
          border-color: ${borderColor};
        }
      }

      @media (min-width: ${breakpoint.min.sm}) {
        > li > a {
          border-bottom-color: ${borderColor};
        }

        > .active > a,
        > .active > a:hover,
        > .active > a:focus {
          border-bottom-color: ${theme.colors.gray[100]};
        }
      }
    }
  `;
});

export default navTabsStyles;
