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
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';

import Badge from 'components/bootstrap/Badge';
import Icon from 'components/common/Icon';

const StyledBadge = styled(Badge)<{ onClick: () => void }>(
  ({ onClick }) => css`
    cursor: ${onClick ? 'pointer' : 'default'};
  `,
);

const BadgeInner = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 2px;
`;

type Props = {
  onClick?: () => void;
  className?: string;
  title?: string;
  count: number;
  iconName?: React.ComponentProps<typeof Icon>['name'];
};

const CountBadge = (
  { onClick = undefined, className = '', title = undefined, count, iconName = undefined }: Props,
  ref: React.ForwardedRef<HTMLDivElement>,
) => (
  <StyledBadge bsStyle="default" className={className} onClick={onClick} ref={ref} title={title}>
    {iconName ? (
      <BadgeInner>
        {count}
        <Icon name={iconName} size="sm" />
      </BadgeInner>
    ) : (
      count
    )}
  </StyledBadge>
);

export default forwardRef(CountBadge);
