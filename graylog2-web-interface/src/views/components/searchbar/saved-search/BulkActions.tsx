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

import { MenuItem } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

const VIEWS_BULK_DELETE_API_ROUTE = '/views/bulk_delete';

const BulkActions = () => {
  const queryClient = useQueryClient();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const selectedItemsAmount = selectedEntities?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'saved search', 'saved searches');

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}?`)) {
      fetch(
        'POST',
        qualifyUrl(VIEWS_BULK_DELETE_API_ROUTE),
        { entity_ids: selectedEntities },
      ).then(({ failures }) => {
        if (failures?.length) {
          const notDeletedSavedSearchIds = failures.map(({ entity_id }) => entity_id);
          setSelectedEntities(notDeletedSavedSearchIds);
          UserNotification.error(`${notDeletedSavedSearchIds.length} out of ${selectedItemsAmount} selected ${descriptor} could not be deleted.`);
        } else {
          setSelectedEntities([]);
          UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} deleted successfully.`, 'Success');
        }
      }).catch((error) => {
        UserNotification.error(`An error occurred while deleting saved searches. ${error}`);
      }).finally(() => {
        queryClient.invalidateQueries(['saved-searches', 'overview']);
      });
    }
  }, [descriptor, queryClient, selectedItemsAmount, selectedEntities, setSelectedEntities]);

  return (
    <BulkActionsDropdown>
      <MenuItem onSelect={onDelete}>Delete</MenuItem>
    </BulkActionsDropdown>
  );
};

export default BulkActions;
