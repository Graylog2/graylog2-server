import React from 'react';

// eslint-disable-next-line no-restricted-imports
import { NavDropdown as BootstrapNavDropdown } from 'react-bootstrap';
import styled from 'styled-components';

import menuItemStyles from './styles/menuItem';

// const NavDropdown = styled(BootstrapNavDropdown)`
//   ${menuItemStyles()};
// `;

const NavDropdown = React.forwardRef((props, ref) => {
  const StyledNavDropdown = React.useCallback(styled(BootstrapNavDropdown)`
   ${menuItemStyles()};
 `, []);

  return (
    <StyledNavDropdown {...props} ref={ref} />
  );
});

export default NavDropdown;
