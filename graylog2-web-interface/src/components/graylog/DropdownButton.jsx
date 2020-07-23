import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { buttonStyles } from './Button';
import menuItemStyles from './styles/menuItem';

const StyledDropdownButton = styled(BootstrapDropdownButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)}
  & ~ {
    ${menuItemStyles}
  }
`);

const DropdownButton = React.memo((props) => <StyledDropdownButton {...props} />);

/** @component */
export default DropdownButton;
