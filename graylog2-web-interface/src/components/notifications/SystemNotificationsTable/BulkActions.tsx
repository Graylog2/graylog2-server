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

import { MenuItem } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useNotificationBulkDismiss from 'components/notifications/hooks/useNotificationBulkDismiss';

const BulkActions = () => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { mutate: bulkDismiss } = useNotificationBulkDismiss();

  const handleBulkDismiss = () => {
    bulkDismiss(
      { entity_ids: selectedEntities },
      {
        onSuccess: () => setSelectedEntities([]),
      },
    );
  };

  return (
    <BulkActionsDropdown>
      <IfPermitted permissions="notifications:delete">
        <MenuItem onSelect={handleBulkDismiss}>Dismiss</MenuItem>
      </IfPermitted>
    </BulkActionsDropdown>
  );
};

export default BulkActions;
