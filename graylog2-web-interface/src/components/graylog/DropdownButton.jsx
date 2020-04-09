import React, { forwardRef } from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled from 'styled-components';

import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';

const StyledDropdownButton = styled(BootstrapDropdownButton)(({ theme }) => `
  ${theme.components.buttonStyles};

  & ~ {
    ${menuItemStyles}
  }
`);

const DropdownButton = forwardRef((props, ref) => <StyledDropdownButton {...props} ref={ref} />);

DropdownButton.propTypes = propTypes;
DropdownButton.defaultProps = defaultProps;

/** @component */
export default DropdownButton;
export { StyledDropdownButton };
