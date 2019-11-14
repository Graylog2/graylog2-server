import { css } from 'styled-components';
import { breakpoint, color, util } from 'theme';

const navTabsStyles = () => {
  const borderColor = util.colorLevel(color.tertiary.due, -3);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          &:hover {
            border-color: ${color.secondary.due} ${color.secondary.due} ${borderColor};
            background-color: ${color.secondary.due};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${color.primary.tre};
            background-color: ${color.primary.due};
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
            border-bottom-color: ${color.primary.due};
          }
        }
      }
    }
  `;
};

export default navTabsStyles;
