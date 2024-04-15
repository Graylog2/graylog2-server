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

import { EntityDataTable, NoSearchResult, PaginatedList, QueryHelper, RelativeTime, SearchForm, Spinner } from 'components/common';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableEventHandlers from 'components/common/EntityDataTable/hooks/useTableEventHandlers';

import EventDefinitionActions from './EventDefinitionActions';
import SchedulingCell from './SchedulingCell';
import StatusCell from './StatusCell';
import BulkActions from './BulkActions';

import type { EventDefinition } from '../event-definitions-types';
import useEventDefinitions from '../hooks/useEventDefinitions';
import { ENTITY_TABLE_ID, DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES } from '../constants';

const customColumnRenderers = (refetchEventDefinitions: () => void): ColumnRenderers<EventDefinition> => ({
  attributes: {
    title: {
      renderCell: (title: string, eventDefinition) => (
        <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{title}</Link>
      ),
    },
    matched_at: {
      renderCell: (_matched_at: string, eventDefinition) => (
        eventDefinition.matched_at ? <RelativeTime dateTime={eventDefinition.matched_at} /> : 'Never'
      ),
    },
    scheduling: {
      renderCell: (_scheduling: string, eventDefinition) => (
        <SchedulingCell definition={eventDefinition} />
      ),
    },
    status: {
      renderCell: (_status: string, eventDefinition) => (
        <StatusCell eventDefinition={eventDefinition} refetchEventDefinitions={refetchEventDefinitions} />
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
  const columnRenderers = customColumnRenderers(refetchEventDefinitions);
  const columnDefinitions = useMemo(
    () => ([...(paginatedEventDefinitions?.attributes ?? []), ...ADDITIONAL_ATTRIBUTES]),
    [paginatedEventDefinitions?.attributes],
  );

  const {
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onColumnsChange,
    onSortChange,
  } = useTableEventHandlers({
    paginationQueryParameter,
    updateTableLayout,
    setQuery,
    appSection: 'event-definition-list',
  });

  const renderEventDefinitionActions = useCallback((listItem: EventDefinition) => (
    <EventDefinitionActions eventDefinition={listItem} refetchEventDefinitions={refetchEventDefinitions} />
  ), [refetchEventDefinitions]);

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
                    onReset={onSearchReset}
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
                                            bulkSelection={{ actions: <BulkActions /> }}
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
