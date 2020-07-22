// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import navTabsStyles from './styles/nav-tabs';

const Nav = styled(BootstrapNav)(({ theme }) => css`
  &.nav {
    > li {
      > a {
        transition: background-color 150ms ease-in-out;

        &:hover,
        &:focus {
          background-color: ${theme.colors.variant.lightest.default};
        }
      }

      &.disabled > a {
        color: ${theme.colors.variant.light.default};

        &:hover,
        &:focus {
          color: ${theme.colors.variant.light.default};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: ${theme.colors.variant.lightest.default};
        border-color: ${theme.colors.variant.primary};
      }
    }

    &.nav-pills {
      > li {
        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.colors.global.textAlt};
            background-color: ${theme.colors.variant.primary};
          }
        }
      }
    }

    ${navTabsStyles}
  }
`);

/** @component */
export default Nav;
