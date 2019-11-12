// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled from 'styled-components';

import teinte from 'theme/teinte';
import navTabsStyles from './styles/nav-tabs';

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

  &${navTabsStyles()}
`;

export default Nav;
