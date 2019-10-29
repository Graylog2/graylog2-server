import React, { forwardRef, useCallback } from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const DropdownButton = forwardRef((props, ref) => {
  const { bsStyle, active, disabled } = props;
  const StyledDropdownButton = useCallback(styled(BootstrapDropdownButton)`
    ${buttonStyles(props)};
  `, [bsStyle, active, disabled]);

  return (
    <StyledDropdownButton ref={ref} {...props} />
  );
});

DropdownButton.propTypes = propTypes;

DropdownButton.defaultProps = defaultProps;

export default DropdownButton;
