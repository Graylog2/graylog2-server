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
          background-color: ${color.gray[90]};
        }
      }

      &.disabled > a {
        color: ${color.global.textDefault};

        &:hover,
        &:focus {
          color: ${color.global.textDefault};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: ${color.gray[90]};
        border-color: ${color.variant.primary};
      }

      .open > a {
        &,
        &:hover,
        &:focus {
          color: ${color.gray[100]};
          background-color: ${color.variant.primary};
        }
      }
    }

    &${navTabsStyles()};
  }
`;

export default Nav;
