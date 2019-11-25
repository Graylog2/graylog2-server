import React, { forwardRef, memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';
import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const StyledDropdownButton = memo(styled(BootstrapDropdownButton)(({ theme }) => css`
  ${buttonStyles(theme)};
  ${menuItemStyles(theme.color, { sibling: true })};
`));

const DropdownButton = forwardRef((props, ref) => {
  return (
    <StyledDropdownButton ref={ref} {...props} />
  );
});

DropdownButton.propTypes = propTypes;

DropdownButton.defaultProps = defaultProps;


export default DropdownButton;
