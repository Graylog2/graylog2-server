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
import type { Sort } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

// Import built-in Event Notification Types
import '../event-notification-types';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { ENTITY_TABLE_ID, DEFAULT_LAYOUT } from 'components/event-notifications/event-notifications/Constants';

import NotificationConfigTypeCell from './NotificationConfigTypeCell';
import NotificationTitle from './NotificationTitle';
import EventNotificationActions from './EventNotificationActions';
import BulkActions from './BulkActions';

import useEventNotifications from '../hooks/useEventNotifications';
import useNotificationTest from '../hooks/useNotificationTest';

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
  const [query, setQuery] = useState('');
  const { layoutConfig, isLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const {
    data: paginatedEventNotifications,
    refetch: refetchEventNotifications,
    isInitialLoading: isLoadingEventNotifications,
  } = useEventNotifications({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  });
  const { isLoadingTest, testResults, getNotificationTest } = useNotificationTest();
  const columnRenderers = useMemo(() => customColumnRenderers(testResults), [testResults]);
  const columnDefinitions = useMemo(
    () => ([...(paginatedEventNotifications?.attributes ?? [])]),
    [paginatedEventNotifications?.attributes],
  );

  const onPageSizeChange = useCallback((newPageSize: number) => {
    paginationQueryParameter.setPagination({ page: 1, pageSize: newPageSize });
    updateTableLayout({ perPage: newPageSize });
  }, [paginationQueryParameter, updateTableLayout]);

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

  const onSortChange = useCallback((newSort: Sort) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ sort: newSort });
  }, [paginationQueryParameter, updateTableLayout]);

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

  if (isLoadingLayoutPreferences || isLoadingEventNotifications) {
    return <Spinner />;
  }

  const { elements, pagination: { total } } = paginatedEventNotifications;

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
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
                                              visibleColumns={layoutConfig.displayedAttributes}
                                              columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                              onColumnsChange={onColumnsChange}
                                              onSortChange={onSortChange}
                                              bulkActions={renderBulkActions}
                                              activeSort={layoutConfig.sort}
                                              onPageSizeChange={onPageSizeChange}
                                              pageSize={layoutConfig.pageSize}
                                              rowActions={renderEventDefinitionActions}
                                              columnRenderers={columnRenderers}
                                              columnDefinitions={columnDefinitions} />
        )}
      </div>
    </PaginatedList>
  );
};

export default EventNotificationsContainer;
