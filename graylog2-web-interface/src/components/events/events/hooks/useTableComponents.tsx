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
import React, { useCallback, useMemo } from 'react';

import EventActions from 'components/events/events/EventActions';
import type { Event } from 'components/events/events/types';
import ExpandedSection from 'components/events/ExpandedSection';
import type useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';

const useTableElements = ({ defaultLayout }: {
  defaultLayout: Parameters<typeof useTableLayout>[0],
}) => {
  const entityActions = useCallback((event: Event) => (
    <EventActions event={event} />
  ), []);

  const renderExpandedRules = useCallback((event: Event) => (
    <ExpandedSection defaultLayout={defaultLayout} event={event} />
  ), [defaultLayout]);

  const expandedSections = useMemo(() => ({
    restFieldsExpandedSection: {
      title: 'Details',
      content: renderExpandedRules,
    },
  }), [renderExpandedRules]);

  return {
    entityActions,
    expandedSections,
  };
};

export default useTableElements;
