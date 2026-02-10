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
import { useCallback, useMemo } from 'react';

import { Events } from '@graylog/server-api';

import useTableElements from 'components/events/events/hooks/useTableComponents';
import { eventsTableElements } from 'components/events/Constants';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import FilterValueRenderers from 'components/events/FilterValueRenderers';
import fetchEvents, { keyFn, parseFilters, getConcatenatedQuery } from 'components/events/fetchEvents';
import type { SearchParams } from 'stores/PaginationTypes';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import useQuery from 'routing/useQuery';
import CustomColumnRenderers from 'components/events/events/ColumnRenderers';
import EventsRefreshControls from 'components/events/events/EventsRefreshControls';
import QueryHelper from 'components/common/QueryHelper';
import EventsWidgets from 'components/events/EventsWidgets';
import EventsRefreshProvider from 'components/events/EventsRefreshProvider';
import PriorityName from 'components/events/events/PriorityName';
import EventTypeLabel from 'components/events/events/EventTypeLabel';

const additionalSearchFields = {
  key: 'The key of the event',
};

const EventsEntityTable = () => {
  const { stream_id: streamId } = useQuery();
  const _fetchEvents = useCallback(
    (searchParams: SearchParams) => fetchEvents(searchParams, streamId as string),
    [streamId],
  );
  const { entityActions, expandedSections, bulkSelection } = useTableElements({
    defaultLayout: eventsTableElements.defaultLayout,
  });

  const _fetchSlices = useCallback(
    (column: string, searchParams: SearchParams) =>
      Events.slices({
        include_all: true,
        slice_column: column,
        parameters: {
          query: getConcatenatedQuery(searchParams.query, streamId as string),
          page: searchParams.page,
          per_page: searchParams.pageSize,
          sort_by: column,
          sort_direction: 'asc',
          sort_unmapped_type: '',
          ...parseFilters(searchParams.filters),
        },
      }).then(({ slices: s }) => [...s]),
    [streamId],
  );

  const sliceRenderers = useMemo(
    () => ({
      // eslint-disable-next-line react/no-unstable-nested-components
      priority: (priority: number) => <PriorityName priority={priority} />,
      // eslint-disable-next-line react/no-unstable-nested-components
      alert: (alert: 'true' | 'false') => <EventTypeLabel isAlert={alert === 'true'} />,
    }),
    [],
  );

  return (
    <EventsRefreshProvider>
      <PaginatedEntityTable<Event, EventsAdditionalData>
        humanName="events"
        queryHelpComponent={<QueryHelper entityName="event" fieldMap={additionalSearchFields} />}
        entityActions={entityActions}
        tableLayout={eventsTableElements.defaultLayout}
        fetchEntities={_fetchEvents}
        fetchSlices={_fetchSlices}
        sliceRenderers={sliceRenderers}
        keyFn={keyFn}
        expandedSectionRenderers={expandedSections}
        entityAttributesAreCamelCase={false}
        filterValueRenderers={FilterValueRenderers}
        columnRenderers={CustomColumnRenderers}
        bulkSelection={bulkSelection}
        topRightCol={<EventsRefreshControls />}
        middleSection={EventsWidgets}
      />
    </EventsRefreshProvider>
  );
};

export default EventsEntityTable;
