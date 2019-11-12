// eslint-disable-next-line no-restricted-imports
import { NavDropdown as BootstrapNavDropdown } from 'react-bootstrap';
import styled from 'styled-components';

import menuItemStyles from './styles/menuItem';

const NavDropdown = styled(BootstrapNavDropdown)`
  ${menuItemStyles()};
`;

export default NavDropdown;
