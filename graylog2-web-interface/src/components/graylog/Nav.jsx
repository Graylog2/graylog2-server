// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { color } from 'theme';
import navTabsStyles from './styles/nav-tabs';

const Nav = styled(BootstrapNav)`
  &.nav {
    > li {
      > a {
        &:hover,
        &:focus {
          background-color: ${color.secondary.due};
        }
      }

      &.disabled > a {
        color: ${color.secondary.tre};

        &:hover,
        &:focus {
          color: ${color.secondary.tre};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: ${color.secondary.due};
        border-color: ${color.tertiary.quattro};
      }

      .open > a {
        &,
        &:hover,
        &:focus {
          color: ${color.primary.due};
          background-color: ${color.tertiary.quattro};
        }
      }
    }

    &${navTabsStyles()};
  }
`;

export default Nav;
