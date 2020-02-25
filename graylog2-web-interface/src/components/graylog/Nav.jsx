// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { util } from 'theme';

const Nav = styled(BootstrapNav)(({ theme }) => {
  const borderColor = util.colorLevel(theme.color.tertiary.due, -3);

  return css`
    &.nav {
      .open > a {
        &,
        &:hover,
        &:focus {
          background-color: ${util.colorLevel(theme.color.secondary.due, -3)};
          border-color: ${borderColor};
        }
      }

      > li {
        > a {
          &:hover,
          &:focus {
            background-color: ${util.colorLevel(theme.color.secondary.due, -3)};
          }
        }

        &.disabled > a {
          color: ${theme.color.secondary.tre};

          &:hover,
          &:focus {
            color: ${theme.color.secondary.tre};
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
            color: ${theme.color.primary.due};
            background-color: ${borderColor};
          }
        }
      }
    }
  `;
});

export default Nav;
