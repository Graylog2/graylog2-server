import React from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

const Button = styled(BootstrapButton)(({ theme }) => css`
  ${theme.components.button}
`);

export default Button;
