import React, { forwardRef, useCallback } from 'react';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const Button = forwardRef(({ active, bsStyle, ...props }, ref) => {
  const StyledButton = useCallback(styled(BootstrapButton)`
    ${buttonStyles({ active })}
  `, [active]);

  return (
    <StyledButton bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Button.propTypes = propTypes;

Button.defaultProps = defaultProps;

export default Button;
