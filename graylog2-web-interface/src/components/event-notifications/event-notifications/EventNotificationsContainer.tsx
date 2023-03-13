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
import type { EventNotification, TestResults } from 'stores/event-notifications/EventNotificationsStore';
import type { SearchParams, Sort } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

// Import built-in Event Notification Types
import '../event-notification-types';

import NotificationConfigTypeCell from './NotificationConfigTypeCell';
import NotificationTitle from './NotificationTitle';
import EventNotificationActions from './EventNotificationActions';
import BulkActions from './BulkActions';

import useEventNotifications from '../hooks/useEventNotifications';
import useNotificationTest from '../hooks/useNotificationTest';

const INITIAL_COLUMNS = ['title', 'description', 'type', 'created_at'];
const COLUMNS_ORDER = ['title', 'description', 'type', 'created_at'];
const customColumnRenderers = (testResults: TestResults): ColumnRenderers<EventNotification> => ({
  attributes: {
    title: {
      renderCell: (_title: string, notification) => {
        return <NotificationTitle notification={notification} testResults={testResults} />;
      },
    },
    type: {
      renderCell: (_type: string, notification) => (
        <NotificationConfigTypeCell notification={notification} />
      ),
    },
  },
});

const EventNotificationsContainer = () => {
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
  const { data: paginatedEventNotifications, refetch: refetchEventNotifications } = useEventNotifications(searchParams);
  const { isLoadingTest, testResults, getNotificationTest } = useNotificationTest();
  const columnRenderers = useMemo(() => customColumnRenderers(testResults), [testResults]);
  const columnDefinitions = useMemo(
    () => ([...(paginatedEventNotifications?.attributes ?? [])]),
    [paginatedEventNotifications?.attributes],
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

  const handleTest = useCallback((notification: EventNotification) => {
    getNotificationTest(notification);
    refetchEventNotifications();
  }, [getNotificationTest, refetchEventNotifications]);

  const renderEventDefinitionActions = useCallback((listItem: EventNotification) => (
    <EventNotificationActions notification={listItem}
                              refetchEventNotification={refetchEventNotifications}
                              isTestLoading={isLoadingTest}
                              onTest={handleTest} />
  ), [handleTest, isLoadingTest, refetchEventNotifications]);

  const renderBulkActions = (
    selectedNotificationsIds: Array<string>,
    setSelectedNotificationsIds: (eventDefinitionsId: Array<string>) => void,
  ) => (
    <BulkActions selectedNotificationsIds={selectedNotificationsIds}
                 setSelectedNotificationsIds={setSelectedNotificationsIds}
                 refetchEventNotifications={refetchEventNotifications} />
  );

  if (!paginatedEventNotifications) {
    return <Spinner text="Loading Notifications information..." />;
  }

  const { elements, pagination: { total } } = paginatedEventNotifications;

  return (
    <PaginatedList onChange={onPageChange}
                   pageSize={searchParams.pageSize}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onReset}
                    queryHelpComponent={<QueryHelper entityName="notification" />} />
      </div>
      <div>
        {elements?.length === 0 ? (
          <NoSearchResult>No notification has been found</NoSearchResult>
        ) : (
          <EntityDataTable<EventNotification> data={elements}
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

export default EventNotificationsContainer;
