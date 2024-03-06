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

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import { EntityDataTable, NoSearchResult, PaginatedList, QueryHelper, SearchForm, Spinner } from 'components/common';
import type { EventNotification, TestResults } from 'stores/event-notifications/EventNotificationsStore';
import type { Sort } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { ENTITY_TABLE_ID, DEFAULT_LAYOUT } from 'components/event-notifications/event-notifications/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

import NotificationConfigTypeCell from './NotificationConfigTypeCell';
import NotificationTitle from './NotificationTitle';
import EventNotificationActions from './EventNotificationActions';
import BulkActions from './BulkActions';

import useEventNotifications from '../hooks/useEventNotifications';
import useNotificationTest from '../hooks/useNotificationTest';

// Import built-in Event Notification Types
import '../event-notification-types';

const customColumnRenderers = (testResults: TestResults): ColumnRenderers<EventNotification> => ({
  attributes: {
    title: {
      renderCell: (_title: string, notification) => <NotificationTitle notification={notification} testResults={testResults} />,
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
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
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
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
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
    sendTelemetry(TELEMETRY_EVENT_TYPE.NOTIFICATIONS.ROW_ACTION_TEST_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-notification',
      app_action_value: 'notification-test',
    });

    getNotificationTest(notification);
    refetchEventNotifications();
  }, [getNotificationTest, pathname, refetchEventNotifications, sendTelemetry]);

  const renderEventDefinitionActions = useCallback((listItem: EventNotification) => (
    <EventNotificationActions notification={listItem}
                              refetchEventNotification={refetchEventNotifications}
                              isTestLoading={isLoadingTest}
                              onTest={handleTest} />
  ), [handleTest, isLoadingTest, refetchEventNotifications]);

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
                                              bulkSelection={{ actions: <BulkActions refetchEventNotifications={refetchEventNotifications} /> }}
                                              activeSort={layoutConfig.sort}
                                              onPageSizeChange={onPageSizeChange}
                                              pageSize={layoutConfig.pageSize}
                                              rowActions={renderEventDefinitionActions}
                                              actionsCellWidth={160}
                                              columnRenderers={columnRenderers}
                                              columnDefinitions={columnDefinitions} />
        )}
      </div>
    </PaginatedList>
  );
};

export default EventNotificationsContainer;
