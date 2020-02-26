import { css } from 'styled-components';
import { breakpoint, util } from 'theme';

const navTabsStyles = css(({ theme }) => {
  const borderColor = util.colorLevel(theme.color.tertiary.due, -3);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          &:hover {
            border-color: ${theme.color.secondary.due} ${theme.color.secondary.due} ${borderColor};
            background-color: ${theme.color.secondary.due};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.color.primary.tre};
            background-color: ${theme.color.primary.due};
            border-color: ${borderColor};
            border-bottom-color: transparent;
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

          > .active > a {
            &,
            &:hover,
            &:focus {
              border-bottom-color: ${theme.color.primary.due};
            }
          }
        }
      }
    }
  `;
});

export default navTabsStyles;
