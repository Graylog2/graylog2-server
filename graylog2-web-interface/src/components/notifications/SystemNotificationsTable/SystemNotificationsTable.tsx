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
import { OrderedMap } from 'immutable';

import { PaginatedEntityTable } from 'components/common';
import fetch from 'logic/rest/FetchProvider';
import PaginationURL from 'util/PaginationURL';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { SearchParams, Sort } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import type { NotificationType } from 'components/notifications/types';
import { NOTIFICATIONS_QUERY_KEY, TABLE_KEY } from 'components/notifications/constants';

import expandedSections from './expandedSections';
import BulkActions from './BulkActions';
import TitleCell from './cells/TitleCell';
import DescriptionCell from './cells/DescriptionCell';
import StatusCell from './cells/StatusCell';
import ActorCell from './cells/ActorCell';
import TriggeredAtCell from './cells/TriggeredAtCell';
import ActionsCell from './cells/ActionsCell';

const DEFAULT_FILTERS = OrderedMap({ is_read: ['false'] });

const TABLE_LAYOUT = {
  entityTableId: 'system-notifications',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'triggered_at', direction: 'desc' } as Sort,
  defaultDisplayedAttributes: ['title', 'description', 'is_read', 'actor.name', 'triggered_at'],
  defaultColumnOrder: ['title', 'description', 'is_read', 'actor.name', 'triggered_at'],
};

type FetchResult = PaginatedResponse<NotificationType>;

export const fetchNotifications = (searchParams: SearchParams): Promise<FetchResult> => {
  const url = PaginationURL('/system/notifications/paginated', searchParams.page, searchParams.pageSize, searchParams.query, {
    sort: searchParams.sort.attributeId,
    order: searchParams.sort.direction,
    ...(searchParams.filters ? { filters: FiltersForQueryParams(searchParams.filters) } : {}),
  });

  return fetch<{ elements: NotificationType[]; pagination: { total: number }; attributes: FetchResult['attributes'] }>(
    'GET',
    url,
  ).then(({ elements, pagination, attributes }) => ({
    list: elements,
    pagination,
    attributes,
  }));
};

export const keyFn = (searchParams: SearchParams) => [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY, searchParams];

const bulkSelection = { actions: <BulkActions /> };

const columnRenderers = {
  attributes: {
    title: {
      renderCell: (_title: string, row: NotificationType) => <TitleCell row={row} />,
    },
    description: {
      renderCell: (_description: string, row: NotificationType) => <DescriptionCell row={row} />,
    },
    is_read: {
      renderCell: (_is_read: boolean, row: NotificationType) => <StatusCell row={row} />,
    },
    'actor.name': {
      renderCell: (_actorName: string, row: NotificationType) => <ActorCell row={row} />,
    },
    triggered_at: {
      renderCell: (triggeredAt: string) => <TriggeredAtCell triggeredAt={triggeredAt} />,
    },
  },
};

const renderActions = (row: NotificationType) => <ActionsCell row={row} />;

const SystemNotificationsTable = () => (
  <PaginatedEntityTable<NotificationType>
    humanName="notifications"
    tableLayout={TABLE_LAYOUT}
    fetchEntities={fetchNotifications}
    keyFn={keyFn}
    entityAttributesAreCamelCase={false}
    columnRenderers={columnRenderers}
    entityActions={renderActions}
    expandedSectionRenderers={expandedSections}
    bulkSelection={bulkSelection}
    defaultFilters={DEFAULT_FILTERS}
  />
);

export default SystemNotificationsTable;
