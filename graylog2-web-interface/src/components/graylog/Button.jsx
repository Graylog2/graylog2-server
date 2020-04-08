import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

import buttonStyles from './styles/buttonStyles';
import { propTypes, defaultProps } from './props/button';

const StyledButton = styled(BootstrapButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)}
`);

const Button = forwardRef((props, ref) => <StyledButton {...props} ref={ref} />);

Button.propTypes = propTypes;
Button.defaultProps = defaultProps;

export default Button;
export { StyledButton };
