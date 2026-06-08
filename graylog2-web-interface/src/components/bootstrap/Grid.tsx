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
import * as React from 'react';
import { Container } from '@mantine/core';
import styled from 'styled-components';

type Props = React.PropsWithChildren<{
  fluid?: boolean;
  className?: string;
}>;

// react-bootstrap's `.container-fluid` uses exactly 15px horizontal padding
// and does not set max-width. Mantine's `Container` defaults to
// `--mantine-spacing-md` (≈16.64px) horizontal padding and sets
// `max-width: 100%` when `fluid`. Pin both to match RB exactly.
const StyledContainer = styled(Container)`
  max-width: none;
  padding-left: 15px;
  padding-right: 15px;
`;

const Grid = ({ fluid = false, className, children }: Props) => (
  <StyledContainer fluid={fluid} className={className}>
    {children}
  </StyledContainer>
);

/** @component */
export default Grid;
