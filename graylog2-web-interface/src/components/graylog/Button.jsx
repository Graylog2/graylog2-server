import React from 'react';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const Button = React.forwardRef(({ active, bsStyle, ...props }, ref) => {
  const innerRef = React.useRef(ref);
  const StyledButton = styled(btnProps => <BootstrapButton {...btnProps} ref={innerRef} />)`
    ${buttonStyles({ active })};
  `;

  return (
    <StyledButton bsStyle={bsStyle} {...props} />
  );
});

Button.propTypes = propTypes;

Button.defaultProps = defaultProps;

export default Button;
