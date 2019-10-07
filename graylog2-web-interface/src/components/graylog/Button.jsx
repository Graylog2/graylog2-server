import React from 'react';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const Button = React.forwardRef(({ active, bsStyle, ...props }, ref) => {

  const StyledButton = styled(BootstrapButton)`
    ${buttonStyles({ active })};
  `;

  return (
    <StyledButton active={active} bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Button.propTypes = propTypes;

Button.defaultProps = defaultProps;

export default Button;
