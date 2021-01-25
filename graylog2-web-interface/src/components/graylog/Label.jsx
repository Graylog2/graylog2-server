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
