import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Badge as BootstrapBadge } from 'react-bootstrap';


const StyledBadge = styled(BootstrapBadge)(({ bsStyle, theme }) => {
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

const Badge = forwardRef(({ ...props }, ref) => {
  return (
    <StyledBadge ref={ref} {...props} />
  );
});

export default Badge;
export { StyledBadge };
