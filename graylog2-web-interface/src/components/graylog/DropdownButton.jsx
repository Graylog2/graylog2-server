import React, { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';
import { buttonStyles } from './Button';

const StyledDropdownButton = styled(BootstrapDropdownButton)(({ bsStyle, theme }) => css`
  ${buttonStyles(bsStyle, theme.color)};

  & ~ {
    ${menuItemStyles}
  }
`);

const DropdownButton = memo((props) => <StyledDropdownButton {...props} />);

DropdownButton.propTypes = propTypes;
DropdownButton.defaultProps = defaultProps;

/** @component */
export default DropdownButton;
export { StyledDropdownButton };
