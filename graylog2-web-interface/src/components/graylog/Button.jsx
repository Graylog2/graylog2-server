import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

import { propTypes, defaultProps } from './props/button';

const StyledButton = styled(BootstrapButton)(({ theme }) => css` ${theme.components.button} `);

const Button = forwardRef((props, ref) => <StyledButton {...props} ref={ref} />);

Button.propTypes = propTypes;
Button.defaultProps = defaultProps;

export default Button;
export { StyledButton };
