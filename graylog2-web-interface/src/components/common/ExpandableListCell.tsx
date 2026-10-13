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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { Badge } from 'components/bootstrap';
import { IconButton } from 'components/common';

const Wrapper = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: flex-start;
    gap: ${theme.spacings.xxs};
    width: 100%;
    white-space: nowrap;

    @media (max-width: 991px) {
      flex-direction: column;
    }
  `,
);

const Items = styled.div<{ $isExpanded: boolean }>(
  ({ theme, $isExpanded }) => css`
    display: flex;
    flex-direction: column;
    flex-wrap: ${$isExpanded ? 'wrap' : 'nowrap'};
    align-items: flex-start;
    flex-grow: 1;
    gap: ${$isExpanded ? theme.spacings.sm : theme.spacings.xs};
    min-width: 0;
    max-width: 100%;
    max-height: ${$isExpanded ? '400px' : 'none'};
    overflow: ${$isExpanded ? 'auto' : 'hidden'};
    text-overflow: ellipsis;
    white-space: ${$isExpanded ? 'normal' : 'nowrap'};
  `,
);

const ToggleButton = styled(IconButton)`
  flex-shrink: 0;
`;

const CountBadge = styled(Badge)`
  flex-shrink: 0;
`;

type Props = {
  // Sorted and keyed by the caller; only the first one shows while collapsed.
  items: Array<React.ReactElement>;
};

const ExpandableListCell = ({ items }: Props) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (items.length === 0) return null;

  const hasMore = items.length > 1;
  const visibleItems = isExpanded ? items : items.slice(0, 1);

  return (
    <Wrapper>
      {hasMore && (
        <ToggleButton
          title="Toggle associated entities"
          name={isExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
          onClick={() => setIsExpanded(!isExpanded)}
        />
      )}
      <Items $isExpanded={isExpanded}>{visibleItems}</Items>
      {!isExpanded && hasMore && <CountBadge>+ {items.length - 1}</CountBadge>}
    </Wrapper>
  );
};

export default ExpandableListCell;
