/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { forwardRef } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Label as BootstrapLabel } from 'react-bootstrap';

const getColorStyles = (theme, bsStyle) => {
  if (!bsStyle) {
    return '';
  }

  const backgroundColor = theme.colors.variant[bsStyle];
  const textColor = theme.utils.contrastingColor(backgroundColor);

  return css`
    background-color: ${backgroundColor};
    color: ${textColor};
`;
};

const StyledLabel = styled(BootstrapLabel)(({ bsStyle, theme }) => css`
  ${getColorStyles(theme, bsStyle)}
  padding: 0.3em 0.6em;
`);

const Label = forwardRef(({ ...props }, ref) => (
  <StyledLabel ref={ref} {...props} />
));

export default Label;
