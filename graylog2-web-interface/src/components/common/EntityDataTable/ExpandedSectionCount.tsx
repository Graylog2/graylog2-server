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
import styled from 'styled-components';

import Badge from 'components/bootstrap/Badge';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { formatReadableNumber } from 'util/NumberFormatting';

// Points to the details below the row. A chevron would suggest the cell itself expands.
const DETAILS_ICON = 'table_rows';

// Static count badges are gray. This one is blue like a link, so it reads as clickable.
const CountButton = styled(Badge)`
  &:hover,
  &:focus-visible {
    text-decoration: underline;
  }
`;

type Props = {
  entityId: string;
  section: string;
  count: number;
  // Names the section's content in the button title, e.g. "stream rules" for "Show stream rules".
  label: string;
  abbreviate?: boolean;
};

/**
 * Count of the items in an expanded section. Clicking it opens the row on that section.
 * It has no chevron, since the row expand column is the control for expanding rows.
 */
const ExpandedSectionCount = (
  { entityId, section, count, label, abbreviate = false }: Props,
  ref: React.ForwardedRef<HTMLElement>,
) => {
  const { expandedSections, toggleSection } = useExpandedSections();
  const isActive = expandedSections?.[entityId]?.includes(section) ?? false;
  const title = `${isActive ? 'Hide' : 'Show'} ${label}`;

  return (
    <CountButton
      ref={ref}
      color="primary"
      variant={isActive ? 'filled' : 'light'}
      rightIcon={DETAILS_ICON}
      onClick={() => toggleSection(entityId, section)}
      title={title}
      aria-label={title}>
      {abbreviate ? formatReadableNumber(count) : count}
    </CountButton>
  );
};

export default forwardRef(ExpandedSectionCount);
