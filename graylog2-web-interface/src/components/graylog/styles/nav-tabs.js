import { css } from 'styled-components';
import { breakpoint, teinte, util } from 'theme';


const navTabsStyles = () => {
  const borderColor = util.colorLevel(teinte.tertiary.due, -3);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          &:hover {
            border-color: ${teinte.secondary.due} ${teinte.secondary.due} ${borderColor};
            background-color: ${teinte.secondary.due};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${teinte.primary.tre};
            background-color: ${teinte.primary.due};
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
            border-bottom-color: ${teinte.primary.due};
          }
        }
      }
    }
  `;
};

export default navTabsStyles;
