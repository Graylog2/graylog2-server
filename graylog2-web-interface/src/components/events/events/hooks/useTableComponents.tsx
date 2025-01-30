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
import React, { useCallback, useMemo, useState } from 'react';
import pickBy from 'lodash/pickBy';
import keyBy from 'lodash/keyBy';

import EventActions from 'components/events/events/EventActions';
import type { Event } from 'components/events/events/types';
import ExpandedSection from 'components/events/ExpandedSection';
import BulkActions from 'components/events/events/BulkActions';
import type { DefaultLayout } from 'components/common/EntityDataTable/types';

const useTableElements = ({ defaultLayout }: {
  defaultLayout: DefaultLayout,
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

  const [selectedEntitiesData, setSelectedEntitiesData] = useState<{[eventId: string]: Event}>({});
  const bulkSelection = useMemo(() => ({
    onChangeSelection: (selectedItemsIds: Array<string>, list: Array<Event>) => {
      setSelectedEntitiesData((cur) => {
        const selectedItemsIdsSet = new Set(selectedItemsIds);
        const filtratedCurrentItems: {[eventId: string]: Event} = pickBy(cur, (_, eventId) => selectedItemsIdsSet.has(eventId));
        const filtratedCurrentEntries = list.filter(({ id }) => selectedItemsIdsSet.has(id));
        const listOfCurrentEntries: {[eventId: string]: Event} = keyBy(filtratedCurrentEntries, 'id');

        return ({ ...filtratedCurrentItems, ...listOfCurrentEntries });
      });
    },
    actions: <BulkActions selectedEntitiesData={selectedEntitiesData} />,

  }), [selectedEntitiesData]);

  return {
    entityActions,
    expandedSections,
    bulkActions: <BulkActions selectedEntitiesData={selectedEntitiesData} />,
    bulkSelection,
  };
};

export default useTableElements;
