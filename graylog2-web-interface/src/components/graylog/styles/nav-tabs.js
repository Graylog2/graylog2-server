import { css } from 'styled-components';
import { breakpoint, color, util } from 'theme';

const navTabsStyles = () => {
  const borderColor = util.colorLevel(color.variant.info, -3);

  return css`
    .nav-tabs {
      border-bottom-color: ${borderColor};

      > li {
        > a {
          &:hover {
            border-color: ${color.gray[90]} ${color.gray[90]} ${borderColor};
            background-color: ${color.gray[90]};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${color.gray[10]};
            background-color: ${color.gray[100]};
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
            border-bottom-color: ${color.gray[100]};
          }
        }
      }
    }
  `;
};

export default navTabsStyles;
