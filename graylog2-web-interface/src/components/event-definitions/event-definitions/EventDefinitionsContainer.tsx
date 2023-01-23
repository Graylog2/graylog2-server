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
import { useState, useMemo, useCallback } from 'react';

import { EntityDataTable, NoSearchResult, PaginatedList, QueryHelper, SearchForm, Spinner } from 'components/common';
import type { SearchParams, Sort } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import 'components/event-definitions/event-definition-types';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import EventDefinitionActions from './EventDefinitionActions';
import SchedulingCell from './SchedulingCell';
import StatusCell from './StatusCell';

import type { EventDefinition } from '../event-definitions-types';
import useEventDefinitions from '../hooks/useEventDefinitions';

const CUSTOM_COLUMN_DEFINITIONS = [
  { id: 'title', title: 'Event Definition title', sortable: true },
  { id: 'scheduling', title: 'Scheduling', sortable: false },
];

const INITIAL_COLUMNS = ['title', 'description', 'priority', 'scheduling', 'status'];
const COLUMNS_ORDER = ['title', 'description', 'priority', 'status', 'scheduling', 'created_at'];
const customColumnRenderers = (): ColumnRenderers<EventDefinition> => ({
  title: {
    renderCell: (eventDefinition) => (
      <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{eventDefinition.title}</Link>
    ),
  },
  scheduling: {
    renderCell: (eventDefinition) => (
      <SchedulingCell definition={eventDefinition} />
    ),
  },
  status: {
    renderCell: (eventDefinition) => (
      <StatusCell status={eventDefinition?.scheduler?.is_scheduled} />
    ),
  },
});

const EventDefinitionsContainer = () => {
  const [visibleColumns, setVisibleColumns] = useState(INITIAL_COLUMNS);
  const paginationQueryParameter = usePaginationQueryParameter(undefined, 20);
  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: paginationQueryParameter.page,
    pageSize: paginationQueryParameter.pageSize,
    query: '',
    sort: {
      attributeId: 'title',
      direction: 'asc',
    },
  });
  const { data: paginatedEventDefinitions, refetch: refetchEventDefinitions } = useEventDefinitions(searchParams);
  const columnRenderers = customColumnRenderers();
  const columnDefinitions = useMemo(
    () => ([...(paginatedEventDefinitions?.attributes ?? []), ...CUSTOM_COLUMN_DEFINITIONS]),
    [paginatedEventDefinitions?.attributes],
  );

  const onPageChange = useCallback(
    (newPage: number, newPageSize: number) => setSearchParams((cur) => ({ ...cur, page: newPage, pageSize: newPageSize })),
    [],
  );

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setSearchParams((cur) => ({ ...cur, query: newQuery }));
  }, [paginationQueryParameter]);

  const onReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onColumnsChange = useCallback((newVisibleColumns: Array<string>) => {
    setVisibleColumns(newVisibleColumns);
  }, []);

  const onSortChange = useCallback((newSort: Sort) => {
    setSearchParams((cur) => ({ ...cur, sort: newSort, page: 1 }));
  }, []);

  const renderEventDefinitionActions = useCallback((listItem: EventDefinition) => (
    <EventDefinitionActions eventDefinition={listItem} refetchEventDefinitions={refetchEventDefinitions} />
  ), [refetchEventDefinitions]);

  const renderBulkActions = (
    // selectedEventDefinitionsIds: Array<string>,
    // setSelectedEventDefinitionsIds: (eventDefinitionsId: Array<string>) => void,
  ) => (
    <span>Bulkactions</span>
  );

  if (!paginatedEventDefinitions) {
    return <Spinner text="Loading Event Definitions information..." />;
  }

  const { elements, pagination: { total } } = paginatedEventDefinitions;

  return (
    <PaginatedList onChange={onPageChange}
                   pageSize={searchParams.pageSize}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onReset}
                    queryHelpComponent={<QueryHelper entityName="stream" />} />
      </div>
      <div>
        {elements?.length === 0 ? (
          <NoSearchResult>No streams have been found</NoSearchResult>
        ) : (
          <EntityDataTable<EventDefinition> data={elements}
                                            visibleColumns={visibleColumns}
                                            columnsOrder={COLUMNS_ORDER}
                                            onColumnsChange={onColumnsChange}
                                            onSortChange={onSortChange}
                                            bulkActions={renderBulkActions}
                                            activeSort={searchParams.sort}
                                            rowActions={renderEventDefinitionActions}
                                            columnRenderers={columnRenderers}
                                            columnDefinitions={columnDefinitions} />
        )}
      </div>
    </PaginatedList>

  );
};

export default EventDefinitionsContainer;
