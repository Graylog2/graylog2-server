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
import type { Sort } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import 'components/event-definitions/event-definition-types';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';

import EventDefinitionActions from './EventDefinitionActions';
import SchedulingCell from './SchedulingCell';
import StatusCell from './StatusCell';
import BulkActions from './BulkActions';

import type { EventDefinition } from '../event-definitions-types';
import useEventDefinitions from '../hooks/useEventDefinitions';
import { SYSTEM_EVENT_DEFINITION_TYPE, ENTITY_TABLE_ID, DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES } from '../constants';

const isSystemEventDefinition = (eventDefinition: EventDefinition): boolean => {
  return eventDefinition?.config?.type === SYSTEM_EVENT_DEFINITION_TYPE;
};

const customColumnRenderers = (): ColumnRenderers<EventDefinition> => ({
  attributes: {
    title: {
      renderCell: (title: string, eventDefinition) => (
        <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{title}</Link>
      ),
    },
    scheduling: {
      renderCell: (_scheduling: string, eventDefinition) => (
        <SchedulingCell definition={eventDefinition} />
      ),
    },
    status: {
      renderCell: (_status: string, eventDefinition) => (
        <StatusCell status={eventDefinition?.scheduler?.is_scheduled}
                    isSystemEvent={isSystemEventDefinition(eventDefinition)} />
      ),
      staticWidth: 100,
    },
    priority: {
      staticWidth: 100,
    },
  },
});

const EventDefinitionsContainer = () => {
  const [query, setQuery] = useState('');
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, 20);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const { data: paginatedEventDefinitions, refetch: refetchEventDefinitions, isInitialLoading: isLoadingEventDefinitions } = useEventDefinitions({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  });
  const columnRenderers = customColumnRenderers();
  const columnDefinitions = useMemo(
    () => ([...(paginatedEventDefinitions?.attributes ?? []), ...ADDITIONAL_ATTRIBUTES]),
    [paginatedEventDefinitions?.attributes],
  );

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setQuery(newQuery);
  }, [paginationQueryParameter]);

  const onReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  const onPageSizeChange = useCallback((newPageSize: number) => {
    paginationQueryParameter.setPagination({ page: 1, pageSize: newPageSize });
    updateTableLayout({ perPage: newPageSize });
  }, [paginationQueryParameter, updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ sort: newSort });
  }, [paginationQueryParameter, updateTableLayout]);

  const renderEventDefinitionActions = useCallback((listItem: EventDefinition) => (
    <EventDefinitionActions eventDefinition={listItem} refetchEventDefinitions={refetchEventDefinitions} />
  ), [refetchEventDefinitions]);

  const renderBulkActions = (
    selectedEventDefinitionsIds: Array<string>,
    setSelectedEventDefinitionsIds: (eventDefinitionsId: Array<string>) => void,
  ) => (
    <BulkActions selectedDefinitionsIds={selectedEventDefinitionsIds}
                 setSelectedEventDefinitionsIds={setSelectedEventDefinitionsIds} />
  );

  if (isLoadingLayoutPreferences || isLoadingEventDefinitions) {
    return <Spinner />;
  }

  const { elements, pagination: { total } } = paginatedEventDefinitions;

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onReset}
                    queryHelpComponent={<QueryHelper entityName="event definition" />} />
      </div>
      <div>
        {elements?.length === 0 ? (
          <NoSearchResult>No Event Definition has been found</NoSearchResult>
        ) : (
          <EntityDataTable<EventDefinition> data={elements}
                                            visibleColumns={layoutConfig.displayedAttributes}
                                            columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                            onColumnsChange={onColumnsChange}
                                            onSortChange={onSortChange}
                                            onPageSizeChange={onPageSizeChange}
                                            pageSize={layoutConfig.pageSize}
                                            bulkActions={renderBulkActions}
                                            activeSort={layoutConfig.sort}
                                            actionsCellWidth={160}
                                            rowActions={renderEventDefinitionActions}
                                            columnRenderers={columnRenderers}
                                            columnDefinitions={columnDefinitions}
                                            entityAttributesAreCamelCase={false} />
        )}
      </div>
    </PaginatedList>

  );
};

export default EventDefinitionsContainer;
