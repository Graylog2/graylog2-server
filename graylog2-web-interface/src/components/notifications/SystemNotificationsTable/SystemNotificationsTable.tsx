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
import type { Sort } from 'stores/PaginationTypes';
import type { NotificationType } from 'components/notifications/types';
import { fetchNotifications, keyFn } from 'components/notifications/hooks/useNotificationsList';

import expandedSections from './expandedSections';
import BulkActions from './BulkActions';
import ActionsCell from './cells/ActionsCell';
import customColumnRenderers from './customColumnRenderers';

const DEFAULT_FILTERS = OrderedMap({ is_read: ['false'] });

const TABLE_LAYOUT = {
  entityTableId: 'system-notifications',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'triggered_at', direction: 'desc' } as Sort,
  defaultDisplayedAttributes: ['title', 'description', 'is_read', 'actor.name', 'triggered_at'],
  defaultColumnOrder: ['title', 'description', 'is_read', 'actor.name', 'triggered_at'],
};

const bulkSelection = { actions: <BulkActions /> };

const renderActions = (row: NotificationType) => <ActionsCell row={row} />;

const SystemNotificationsTable = () => (
  <PaginatedEntityTable<NotificationType>
    humanName="notifications"
    tableLayout={TABLE_LAYOUT}
    fetchEntities={fetchNotifications}
    keyFn={keyFn}
    entityAttributesAreCamelCase={false}
    columnRenderers={customColumnRenderers}
    entityActions={renderActions}
    expandedSectionRenderers={expandedSections}
    bulkSelection={bulkSelection}
    defaultFilters={DEFAULT_FILTERS}
    fetchOptions={{ retry: false }}
  />
);

export default SystemNotificationsTable;
