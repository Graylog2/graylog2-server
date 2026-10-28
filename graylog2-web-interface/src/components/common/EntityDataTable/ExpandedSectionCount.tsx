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

import { Badge } from 'components/bootstrap';
import Icon from 'components/common/Icon';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { formatReadableNumber } from 'util/NumberFormatting';

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
 */
// Forwards the ref, since cells wrap the count in a tooltip.
const ExpandedSectionCount = (
  { entityId, section, count, label, abbreviate = false }: Props,
  ref: React.ForwardedRef<HTMLElement>,
) => {
  const { expandedSections, toggleSection } = useExpandedSections();
  const isActive = expandedSections?.[entityId]?.includes(section) ?? false;

  return (
    <Badge
      ref={ref}
      color={isActive ? 'primary' : undefined}
      variant={isActive ? 'filled' : undefined}
      onClick={() => toggleSection(entityId, section)}
      title={`${isActive ? 'Hide' : 'Show'} ${label}`}>
      {abbreviate ? formatReadableNumber(count) : count}
      <Icon name="dock_to_bottom" size="xs" type="regular" />
    </Badge>
  );
};

export default forwardRef(ExpandedSectionCount);
