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
import styled, { css } from 'styled-components';

import { Badge } from 'components/bootstrap';
import Icon from 'components/common/Icon';

// Below the expanded items, the badge stays at the start instead of stretching across the cell.
const ToggleBadge = styled(Badge)<{ $isExpanded: boolean }>(
  ({ $isExpanded }) => css`
    flex-shrink: 0;
    ${$isExpanded &&
    css`
      align-self: flex-start;
    `}
  `,
);

type Props = {
  isExpanded: boolean;
  hiddenCount: number;
  onToggle: (e: React.MouseEvent) => void;
};

const CellListToggle = ({ isExpanded, hiddenCount, onToggle }: Props) => {
  if (isExpanded) {
    return (
      <ToggleBadge color="gray" size="sm" onClick={onToggle} $isExpanded>
        Show less
        <Icon name="keyboard_arrow_up" size="sm" />
      </ToggleBadge>
    );
  }

  const title = `Show ${hiddenCount} more`;

  return (
    <ToggleBadge color="gray" size="sm" onClick={onToggle} title={title} aria-label={title} $isExpanded={false}>
      +{hiddenCount}
      <Icon name="keyboard_arrow_down" size="sm" />
    </ToggleBadge>
  );
};

export default CellListToggle;
