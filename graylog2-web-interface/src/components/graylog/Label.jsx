import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Label as BootstrapLabel } from 'react-bootstrap';


const StyledLabel = styled(BootstrapLabel)(({ bsStyle, theme }) => {
  if (!bsStyle) {
    return undefined;
  }

  const backgroundColor = theme.colors.variant[bsStyle];
  const textColor = theme.utils.readableColor(backgroundColor);

  return css`
    background-color: ${backgroundColor};
    color: ${textColor};
  `;
});

const Label = forwardRef(({ ...props }, ref) => {
  return (
    <StyledLabel ref={ref} {...props} />
  );
});

export default Label;
