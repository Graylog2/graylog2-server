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
import React, { useMemo, useCallback } from 'react';

import useCurrentUser from 'hooks/useCurrentUser';
import useTableElements from 'components/events/useTableComponents';
import CustomColumnRenderers from 'components/events/ColumnRenderers';
import getStreamTableElements from 'components/events/Constants';
import PaginatedEntityTable, { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import FilterValueRenderers from 'components/events/FilterValueRenderers';
import type { EventsAdditionalData } from 'components/events/fetchEvents';
import fetchEvents, { keyFn } from 'components/events/fetchEvents';
import type { SearchParams } from 'stores/PaginationTypes';
import type { Event } from 'components/events/events/types';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import useQuery from 'routing/useQuery';
import AutoRefreshProvider from 'views/components/contexts/AutoRefreshProvider';
import EventsRefreshControls from 'components/events/events/EventsRefreshControls';

import QueryHelper from '../common/QueryHelper';

const NewItem = () => {
  const { refetch } = useTableFetchContext();

  return <AutoRefreshProvider onRefresh={refetch}><EventsRefreshControls disable={false} /></AutoRefreshProvider>;
};

const EventsEntityTable = () => {
  const currentUser = useCurrentUser();
  const { stream_id: streamId } = useQuery();

  const columnRenderers = useMemo(() => CustomColumnRenderers(currentUser.permissions), [currentUser.permissions]);
  const { columnOrder, defaultLayout } = useMemo(() => getStreamTableElements(), []);
  const _fetchEvents = useCallback((searchParams: SearchParams): Promise<PaginatedResponse<Event, EventsAdditionalData>> => fetchEvents(searchParams, streamId as string), [streamId]);
  const { entityActions, expandedSections } = useTableElements({ defaultLayout });

  return (
    <PaginatedEntityTable<Event> humanName="events"
                                 columnsOrder={columnOrder}
                                 queryHelpComponent={<QueryHelper entityName="events" />}
                                 entityActions={entityActions}
                                 tableLayout={defaultLayout}
                                 fetchEntities={_fetchEvents}
                                 keyFn={keyFn}
                                 actionsCellWidth={100}
                                 expandedSectionsRenderer={expandedSections}
                                  // bulkSelection={{ actions: bulkActions }}
                                 entityAttributesAreCamelCase={false}
                                 filterValueRenderers={FilterValueRenderers}
                                 columnRenderers={columnRenderers}
                                 topRightCol={<NewItem />} />
  );
};

export default EventsEntityTable;
