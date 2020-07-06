import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

const StyledButton = styled(BootstrapButton)(({ theme }) => css` ${theme.components.button} `);

const Button = forwardRef((props, ref) => <StyledButton {...props} ref={ref} />);

export default Button;
export { StyledButton };
