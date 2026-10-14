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

import { DeleteMenuItem } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

const BulkActions = () => {
  const { t } = useTranslation('search');
  const queryClient = useQueryClient();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const selectedItemsAmount = selectedEntities?.length;

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('saved_searches.confirm_bulk_delete', { count: selectedItemsAmount }))) {
      Views.bulkDelete({ entity_ids: selectedEntities })
        .then(({ failures }) => {
          if (failures?.length) {
            const notDeletedSavedSearchIds = failures.map(({ entity_id }) => entity_id);
            setSelectedEntities(notDeletedSavedSearchIds);
            UserNotification.error(
              t('saved_searches.bulk_delete_partial', {
                count: selectedItemsAmount,
                failed: notDeletedSavedSearchIds.length,
              }),
            );
          } else {
            setSelectedEntities([]);
            UserNotification.success(
              t('saved_searches.bulk_delete_success', { count: selectedItemsAmount }),
              t('saved_searches.success'),
            );
          }
        })
        .catch((error) => {
          UserNotification.error(t('saved_searches.bulk_delete_error', { error }));
        })
        .finally(() => {
          queryClient.invalidateQueries({
            queryKey: ['saved-searches', 'overview'],
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
