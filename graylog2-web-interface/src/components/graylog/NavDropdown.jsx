// eslint-disable-next-line no-restricted-imports
import { NavDropdown as BootstrapNavDropdown } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';

const NavDropdown = styled(BootstrapNavDropdown)(({ theme }) => css`
  ${menuItemStyles(theme.color)};
`);

export default NavDropdown;
