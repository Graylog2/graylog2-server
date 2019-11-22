import { css } from 'styled-components';
import { breakpoint, teinte } from 'theme';

const navTabsStyles = () => css`
  .nav-tabs {
    border-bottom-color: ${teinte.tertiary.quattro};

    > li {
      > a {
        &:hover {
          border-color: ${teinte.secondary.due} ${teinte.secondary.due} ${teinte.tertiary.quattro};
          background-color: ${teinte.secondary.due};
        }
      }

      &.active > a {
        &,
        &:hover,
        &:focus {
          color: ${teinte.primary.tre};
          background-color: ${teinte.primary.due};
          border-color: ${teinte.tertiary.quattro};
          border-bottom-color: transparent;
        }
      }
    }

    &.nav-justified {
      > .active > a,
      > .active > a:hover,
      > .active > a:focus {
        border-color: ${teinte.tertiary.quattro};
      }

      @media (min-width: ${breakpoint.min.sm}) {
        > li > a {
          border-bottom-color: ${teinte.tertiary.quattro};
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

export default navTabsStyles;
