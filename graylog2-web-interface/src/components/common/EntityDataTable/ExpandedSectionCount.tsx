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

import CountBadge from 'components/common/CountBadge';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

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
 * Like the row expand column, it uses a chevron, which in entity tables always means "details below the row".
 */
const ExpandedSectionCount = (
  { entityId, section, count, label, abbreviate = false }: Props,
  ref: React.ForwardedRef<HTMLDivElement>,
) => {
  const { expandedSections, toggleSection } = useExpandedSections();
  const isActive = expandedSections?.[entityId]?.includes(section) ?? false;

  return (
    <CountBadge
      ref={ref}
      count={count}
      abbreviate={abbreviate}
      iconName={isActive ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={() => toggleSection(entityId, section)}
      title={`${isActive ? 'Hide' : 'Show'} ${label}`}
    />
  );
};

export default forwardRef(ExpandedSectionCount);
