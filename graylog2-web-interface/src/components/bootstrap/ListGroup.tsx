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

type OwnProps = {
  bsClass?: string;
  componentClass?: React.ElementType;
};

type Props = OwnProps & React.ComponentPropsWithoutRef<'ul'>;

const ListGroup = ({
  bsClass = undefined,
  children = undefined,
  className = undefined,
  componentClass = undefined,
  style = undefined,
  ...rest
}: Props) => (
  <StyledList
    as={componentClass}
    className={[className, bsClass].filter(Boolean).join(' ') || undefined}
    style={style}
    {...rest}
  >
    {children}
  </StyledList>
);

/** @component */
export default ListGroup;
