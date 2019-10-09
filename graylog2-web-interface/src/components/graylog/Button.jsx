import React from 'react';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const Button = ({ active, bsStyle, innerRef, ...props }) => {
  const ref = React.useRef(innerRef);
  const StyledButton = styled(btnProps => <BootstrapButton {...btnProps} ref={ref} />)`
    ${buttonStyles({ active })};
  `;

  return (
    <StyledButton bsStyle={bsStyle} {...props} />
  );
};

Button.propTypes = propTypes;

Button.defaultProps = defaultProps;

export default Button;
