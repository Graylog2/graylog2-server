import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Jumbotron as BootstrapJumbotron } from 'react-bootstrap';

export const StyledJumbotron = styled(BootstrapJumbotron)(({ theme }) => css`
  color: ${theme.color.global.textDefault};
  background-color: ${theme.color.global.background};
`);

const Jumbotron = forwardRef((props, ref) => {
  return (
    <StyledJumbotron ref={ref} {...props} />
  );
});

export default Jumbotron;
