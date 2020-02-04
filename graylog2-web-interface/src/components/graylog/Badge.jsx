import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Badge as BootstrapBadge } from 'react-bootstrap';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const cssBuilder = (backgroundColor) => {
  const textColor = util.readableColor(backgroundColor);

  return css`
    background-color: ${backgroundColor};
    color: ${textColor};
  `;
};

const StyledBadge = styled(BootstrapBadge)`
  ${bsStyleThemeVariant(cssBuilder)}
`;

const Badge = forwardRef(({ ...props }, ref) => {
  return (
    <StyledBadge ref={ref} {...props} />
  );
});

export default Badge;
