// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { teinte, util } from 'theme';
import navTabsStyles from './styles/nav-tabs';

const Nav = styled(BootstrapNav)(() => {
  const borderColor = util.colorLevel(teinte.tertiary.due, -3);

  return css`
    &.nav {
      .open > a {
        &,
        &:hover,
        &:focus {
          background-color: ${util.colorLevel(teinte.secondary.due, -3)};
          border-color: ${borderColor};
        }
      }

      > li {
        > a {
          &:hover,
          &:focus {
          background-color: ${util.colorLevel(teinte.secondary.due, -3)};
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
        background-color: ${util.colorLevel(teinte.secondary.due, -3)};
          border-color: ${borderColor};
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
            background-color: ${borderColor};
          }
        }
      }
    }

    ${navTabsStyles};
  `;
});

export default Nav;
