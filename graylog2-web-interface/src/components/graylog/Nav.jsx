// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled from 'styled-components';

import teinte from 'theme/teinte';

const Nav = styled(BootstrapNav)`
  &.nav {
    > li {
      > a {
        &:hover,
        &:focus {
          background-color: ${teinte.secondary.due};
        }
      }

      &.disabled > a {
        color: ${teinte.secondary.tre};

        &:hover,
        &:focus {
          color: ${teinte.secondary.tre};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: ${teinte.secondary.due};
        border-color: ${teinte.tertiary.quattro};
      }
    }
  }

  &.nav-tabs {
    border-bottom-color: ${teinte.tertiary.quattro};

    > li {
      > a {
        &:hover {
          border-color: ${teinte.secondary.due} ${teinte.secondary.due} ${teinte.tertiary.quattro};
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
  }

  &.nav-pills {
    > li {
      &.active > a {
        &,
        &:hover,
        &:focus {
          color: ${teinte.primary.due};
          background-color: ${teinte.tertiary.quattro};
        }
      }
    }
  }

  &.nav-tabs.nav-justified {
    > .active > a,
    > .active > a:hover,
    > .active > a:focus {
      border-color: ${teinte.tertiary.quattro};
    }

    @media (min-width: 768px) {
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
`;

export default Nav;
