import React, { forwardRef, useCallback } from 'react';
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const DropdownButton = forwardRef(({ active, bsStyle, ...props }, ref) => {
  const StyledDropdownButton = useCallback(styled(BootstrapDropdownButton)`
    ${buttonStyles({ active })};
  `, [active]);

  return (
    <StyledDropdownButton bsStyle={bsStyle} ref={ref} {...props} />
  );
});

DropdownButton.propTypes = propTypes;

DropdownButton.defaultProps = defaultProps;

export default DropdownButton;
