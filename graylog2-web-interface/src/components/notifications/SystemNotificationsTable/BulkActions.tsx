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
import { useQueryClient } from '@tanstack/react-query';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { NOTIFICATIONS_QUERY_KEY, TABLE_KEY } from 'components/notifications/constants';
import useNotificationBulkToggleRead from 'components/notifications/hooks/useNotificationBulkToggleRead';
import type { PageShape } from 'components/notifications/types';

const BulkActions = () => {
  const queryClient = useQueryClient();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { mutate: bulkToggleRead } = useNotificationBulkToggleRead();
  const tableKey = [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] as const;

  const getSelectedRowSeeds = () => {
    const cachedPages = queryClient.getQueriesData<PageShape>({ queryKey: tableKey });
    const allRows = cachedPages.flatMap(([, data]) => data?.elements ?? []);

    return selectedEntities.map((id) => {
      const row = allRows.find((r) => r.id === id);

      return { id, currentIsRead: row?.is_read ?? false };
    });
  };

  const handleBulkToggle = () => {
    bulkToggleRead({ rows: getSelectedRowSeeds() }, {
      onSuccess: () => setSelectedEntities([]),
    });
  };

  return (
    <BulkActionsDropdown>
      <MenuItem onSelect={handleBulkToggle}>Toggle read state</MenuItem>
    </BulkActionsDropdown>
  );
};

export default BulkActions;
