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
import styled, { css } from 'styled-components';
import * as React from 'react';
import { forwardRef } from 'react';

import { Badge } from 'components/bootstrap';

const StyledBadge = styled(Badge)<{ onClick: () => void }>(
  ({ onClick }) => css`
    cursor: ${onClick ? 'pointer' : 'default'};
  `,
);

type Props = {
  disabled?: boolean;
  children: React.ReactNode;
  onClick?: () => void;
  title: string;
};

const StreamCountBadge = (
  { disabled = false, children, onClick = undefined, title }: Props,
  ref: React.ForwardedRef<HTMLDivElement>,
) => (
  <StyledBadge bsStyle={disabled ? 'gray' : 'info'} onClick={onClick} title={title} ref={ref}>
    {children}
  </StyledBadge>
);

export default forwardRef(StreamCountBadge);
