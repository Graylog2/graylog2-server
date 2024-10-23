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
import { useQueryClient } from '@tanstack/react-query';

import usePipelineColumn from 'components/streams/StreamsOverview/hooks/usePipelineColumn';
import useCurrentUser from 'hooks/useCurrentUser';
import useTableElements from 'components/events/useTableComponents';
import CustomColumnRenderers from 'components/events/ColumnRenderers';
import getStreamTableElements from 'components/events/Constants';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { Stream } from 'stores/streams/StreamsStore';
import FilterValueRenderers from 'components/streams/StreamsOverview/FilterValueRenderers';
import fetchEvents, { keyFn } from 'components/events/events/fetchEvents';
import type { SearchParams } from 'stores/PaginationTypes';

import QueryHelper from '../common/QueryHelper';

const EventsEntityTable = () => {
  const { isPipelineColumnPermitted } = usePipelineColumn();
  const currentUser = useCurrentUser();

  const { entityActions, expandedSections, bulkActions } = useTableElements();

  const columnRenderers = useMemo(() => CustomColumnRenderers(currentUser.permissions, keyFn), [currentUser.permissions]);
  const { columnOrder, additionalAttributes, defaultLayout } = useMemo(() => getStreamTableElements(currentUser.permissions, isPipelineColumnPermitted), [currentUser.permissions, isPipelineColumnPermitted]);
  const _fetchEvents = useCallback((searchParams: SearchParams) => {
    return fetchEvents(searchParams);
  }, []);

  return (
    <PaginatedEntityTable<Stream> humanName="events"
                                  columnsOrder={columnOrder}
                                  additionalAttributes={additionalAttributes}
                                  queryHelpComponent={<QueryHelper entityName="events" />}
                                  entityActions={entityActions}
                                  tableLayout={defaultLayout}
                                  fetchEntities={_fetchEvents}
                                  keyFn={keyFn}
                                  actionsCellWidth={220}
                                  // expandedSectionsRenderer={expandedSections}
                                  // bulkSelection={{ actions: bulkActions }}
                                  entityAttributesAreCamelCase={false}
                                  filterValueRenderers={FilterValueRenderers}
                                  columnRenderers={columnRenderers} />
  );
};

export default EventsEntityTable;
