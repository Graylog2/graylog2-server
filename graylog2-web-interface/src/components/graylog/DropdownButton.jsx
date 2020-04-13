import React, { forwardRef, memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';
import buttonStyles from './styles/buttonStyles';

const StyledDropdownButton = styled(BootstrapDropdownButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)};

  & ~ {
    ${menuItemStyles}
  }
`);

const DropdownButton = memo(forwardRef((props, ref) => <StyledDropdownButton {...props} ref={ref} />));

DropdownButton.propTypes = propTypes;
DropdownButton.defaultProps = defaultProps;

/** @component */
export default DropdownButton;
export { StyledDropdownButton };
