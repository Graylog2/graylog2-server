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
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import { Views } from '@graylog/server-api';

import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import UserNotification from 'util/UserNotification';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { DeleteMenuItem } from 'components/bootstrap';

const BulkActions = () => {
  const { t } = useTranslation('dashboards');
  const queryClient = useQueryClient();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const selectedItemsAmount = selectedEntities?.length;

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('bulk_delete.confirm', { count: selectedItemsAmount }))) {
      Views.bulkDelete({ entity_ids: selectedEntities })
        .then(({ failures }) => {
          if (failures?.length) {
            const notDeletedDashboardIds = failures.map(({ entity_id }) => entity_id);
            setSelectedEntities(notDeletedDashboardIds);
            UserNotification.error(
              t('bulk_delete.partial_failure', { failed: notDeletedDashboardIds.length, count: selectedItemsAmount }),
            );
          } else {
            setSelectedEntities([]);
            UserNotification.success(
              t('bulk_delete.success', { count: selectedItemsAmount }),
              t('bulk_delete.success_title'),
            );
          }
        })
        .catch((error) => {
          UserNotification.error(t('bulk_delete.failed', { error }));
        })
        .finally(() => {
          queryClient.invalidateQueries({
            queryKey: ['dashboards', 'overview'],
          });
        });
    }
  }, [queryClient, selectedItemsAmount, selectedEntities, setSelectedEntities, t]);

  return (
    <BulkActionsDropdown>
      <DeleteMenuItem onSelect={onDelete} />
    </BulkActionsDropdown>
  );
};

export default BulkActions;
