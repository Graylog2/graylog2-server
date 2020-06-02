import { css } from 'styled-components';
import { breakpoint } from 'theme';

const navTabsStyles = css(({ theme }) => {
  const borderColor = theme.utils.colorLevel(theme.color.variant.info, -5);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          transition: background-color 150ms ease-in-out;
          color: ${theme.color.global.textDefault};
          border-color: ${theme.color.gray[80]} ${theme.color.gray[80]} ${borderColor};

          &:hover {
            background-color: ${theme.color.gray[80]};
            color: ${theme.utils.contrastingColor(theme.color.gray[80])};
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

        &.disabled > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.color.gray[60]};
            background-color: ${theme.color.gray[100]};
            border-color: ${theme.color.gray[100]} ${theme.color.gray[100]} ${borderColor};
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
          border-bottom-color: ${theme.color.gray[100]};
        }
      }
    }
  `;
});

export default navTabsStyles;
