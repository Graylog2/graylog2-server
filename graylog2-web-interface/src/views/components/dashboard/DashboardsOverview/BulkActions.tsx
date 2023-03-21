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
import { useCallback } from 'react';

import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import StringUtils from 'util/StringUtils';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import MenuItem from 'components/bootstrap/MenuItem';

const VIEWS_BULK_DELETE_API_ROUTE = '/views/bulk_delete';

type Props = {
  selectedDashboardIds: Array<string>,
  setSelectedDashboardIds: (dashboardIds: Array<string>) => void,
}

const BulkActions = ({ selectedDashboardIds, setSelectedDashboardIds }: Props) => {
  const queryClient = useQueryClient();

  const selectedItemsAmount = selectedDashboardIds?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'dashboard', 'dashboards');

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}?`)) {
      fetch(
        'POST',
        qualifyUrl(VIEWS_BULK_DELETE_API_ROUTE),
        { entity_ids: selectedDashboardIds },
      ).then(({ failures }) => {
        if (failures?.length) {
          const notDeletedDashboardIds = failures.map(({ entity_id }) => entity_id);
          setSelectedDashboardIds(notDeletedDashboardIds);
          UserNotification.error(`${notDeletedDashboardIds.length} out of ${selectedItemsAmount} selected ${descriptor} could not be deleted.`);
        } else {
          setSelectedDashboardIds([]);
          UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} deleted successfully.`, 'Success');
        }
      }).catch((error) => {
        UserNotification.error(`An error occurred while deleting dashboards. ${error}`);
      }).finally(() => {
        queryClient.invalidateQueries(['dashboards', 'overview']);
      });
    }
  }, [descriptor, queryClient, selectedItemsAmount, selectedDashboardIds, setSelectedDashboardIds]);

  return (
    <BulkActionsDropdown selectedEntities={selectedDashboardIds} setSelectedEntities={setSelectedDashboardIds}>
      <MenuItem onSelect={onDelete}>Delete</MenuItem>
    </BulkActionsDropdown>
  );
};

export default BulkActions;
