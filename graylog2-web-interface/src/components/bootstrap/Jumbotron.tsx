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
import React from 'react';
import styled, { css } from 'styled-components';

import { RowContentStyles } from 'components/bootstrap/Row';

export const Container = styled.div(
  ({ theme }) => css`
    ${RowContentStyles}

    padding: ${theme.spacings.lg} ${theme.spacings.md};

    @media (min-width: ${theme.breakpoints.min.sm}) {
      padding: ${theme.spacings.xl} ${theme.spacings.lg};
    }

    @media (min-width: ${theme.breakpoints.min.md}) {
      padding: ${theme.spacings.xl};
    }

    h2 {
      font-weight: bold;
    }

    p {
      margin-bottom: 9px;
      font-size: 20px;
      font-weight: normal;
    }
  `,
);

type JumbotronProps = {
  children: React.ReactNode;
  className?: string;
};

const Jumbotron = ({ children, className = undefined }: JumbotronProps) => (
  <Container className={className}>{children}</Container>
);

export default Jumbotron;
