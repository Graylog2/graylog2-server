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
import styled from 'styled-components';

const StyledList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0;
`;

type Props = {
  'aria-label'?: string;
  bsClass?: string;
  children?: React.ReactNode;
  className?: string;
  componentClass?: React.ElementType;
  'data-testid'?: string;
  style?: React.CSSProperties;
};

const ListGroup = ({
  'aria-label': ariaLabel = undefined,
  bsClass = undefined,
  children = undefined,
  className = undefined,
  componentClass = undefined,
  'data-testid': dataTestId = undefined,
  style = undefined,
}: Props) => (
  <StyledList as={componentClass} aria-label={ariaLabel} className={`${className ?? ''} ${bsClass ?? ''}`} style={style} data-testid={dataTestId}>
    {children}
  </StyledList>
);

/** @component */
export default ListGroup;
