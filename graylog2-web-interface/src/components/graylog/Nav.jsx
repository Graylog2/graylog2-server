// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { util } from 'theme';
import navTabsStyles from './styles/nav-tabs';

const Nav = styled(BootstrapNav)(({ theme }) => css`
  &.nav {
    > li {
      > a {
        &:hover,
        &:focus {
          background-color: ${theme.color.gray[90]};
        }
      }

      &.disabled > a {
        color: ${theme.color.gray[60]};

        &:hover,
        &:focus {
          color: ${theme.color.gray[60]};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: ${theme.color.gray[90]};
        border-color: ${theme.color.variant.primary};
      }
    }

    &.nav-pills {
      > li {
        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.color.global.textAlt};
            background-color: ${util.colorLevel(theme.color.variant.info, -3)};
          }
        }
      }
    }

    ${navTabsStyles};
  }
`);

/** @component */
export default Nav;
