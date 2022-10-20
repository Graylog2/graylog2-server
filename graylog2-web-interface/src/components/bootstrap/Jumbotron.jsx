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
import { Jumbotron as BootstrapJumbotron } from 'react-bootstrap';

export const StyledJumbotron = styled(BootstrapJumbotron)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  background-color: ${theme.colors.global.contentBackground};
  
  h2 {
    font-weight: bold;
  }

  p {
    margin-bottom: 9px;
    font-size: 20px;
    font-weight: normal;
  }
`);

const Jumbotron = forwardRef((props, ref) => {
  return (
    <StyledJumbotron ref={ref} {...props} />
  );
});

export default Jumbotron;
