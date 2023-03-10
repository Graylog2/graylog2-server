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
import styled from 'styled-components';
import { forwardRef } from 'react';

const Badge = styled.span(({ theme }) => `
  background-color: ${theme.colors.variant.light.info};
`);

type Props = {
  children: React.ReactNode;
  onClick?: () => void,
  className?: string,
  title?: string,
};

const CountBadge = forwardRef<HTMLElement, Props>(({ children, onClick, className, title }: Props, ref) => (
  <Badge className={`badge ${className}`} onClick={onClick} ref={ref} title={title}>
    {children}
  </Badge>
));

CountBadge.defaultProps = {
  onClick: undefined,
  className: '',
  title: undefined,
};

export default CountBadge;
