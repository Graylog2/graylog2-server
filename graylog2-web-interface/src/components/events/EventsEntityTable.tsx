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
import React, { useCallback } from 'react';

import useTableElements from 'components/events/events/hooks/useTableComponents';
import { eventsTableElements } from 'components/events/Constants';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import FilterValueRenderers from 'components/events/FilterValueRenderers';
import fetchEvents, { keyFn } from 'components/events/fetchEvents';
import type { SearchParams } from 'stores/PaginationTypes';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import useQuery from 'routing/useQuery';
import useColumnRenderers from 'components/events/events/ColumnRenderers';
import EventsRefreshControls from 'components/events/events/EventsRefreshControls';
import QueryHelper from 'components/common/QueryHelper';

const additionalSearchFields = {
  key: 'The key of the event',
};

const EventsEntityTable = () => {
  const { stream_id: streamId } = useQuery();

  const columnRenderers = useColumnRenderers();
  const _fetchEvents = useCallback((searchParams: SearchParams) => fetchEvents(searchParams, streamId as string), [streamId]);
  const { entityActions, expandedSections, bulkSelection } = useTableElements({ defaultLayout: eventsTableElements.defaultLayout });

  return (
    <PaginatedEntityTable<Event, EventsAdditionalData> humanName="events"
                                                       columnsOrder={eventsTableElements.columnOrder}
                                                       queryHelpComponent={<QueryHelper entityName="event" fieldMap={additionalSearchFields} />}
                                                       entityActions={entityActions}
                                                       tableLayout={eventsTableElements.defaultLayout}
                                                       fetchEntities={_fetchEvents}
                                                       keyFn={keyFn}
                                                       actionsCellWidth={110}
                                                       expandedSectionsRenderer={expandedSections}
                                                       entityAttributesAreCamelCase={false}
                                                       filterValueRenderers={FilterValueRenderers}
                                                       columnRenderers={columnRenderers}
                                                       bulkSelection={bulkSelection}
                                                       topRightCol={<EventsRefreshControls />} />
  );
};

export default EventsEntityTable;
