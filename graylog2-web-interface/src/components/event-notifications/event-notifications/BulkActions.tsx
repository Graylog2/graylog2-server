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
import uniq from 'lodash/uniq';
import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { EventsNotifications } from '@graylog/server-api';

import type FetchError from 'logic/errors/FetchError';
import UserNotification from 'util/UserNotification';
import { DeleteMenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import { EVENT_NOTIFICATIONS_QUERY_KEY } from 'components/event-notifications/hooks/useEventNotifications';

const BulkActions = () => {
  const { t } = useTranslation('event_notifications');
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry('event-notification-bulk');
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const selectedItemsAmount = selectedEntities?.length;
  const { refetch: refetchEventNotifications } = useTableFetchContext();

  const onDelete = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.NOTIFICATIONS.BULK_ACTION_DELETE_CLICKED, {
      app_action_value: 'bulk-delete-button',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm(t('bulk.delete_confirm', { count: selectedItemsAmount }))) {
      const deleteCalls = selectedEntities.map((notificationId) =>
        EventsNotifications.remove(notificationId).then(() => notificationId),
      );

      Promise.allSettled(deleteCalls).then((result) => {
        const fulfilledRequests = result.filter((response) => response.status === 'fulfilled') as Array<{
          status: 'fulfilled';
          value: string;
        }>;
        const deletedNotificationIds = fulfilledRequests.map(({ value }) => value);
        const notDeletedNotificationIds = selectedEntities?.filter(
          (streamId) => !deletedNotificationIds.includes(streamId),
        );

        if (notDeletedNotificationIds.length) {
          const rejectedRequests = result.filter((response) => response.status === 'rejected') as Array<{
            status: 'rejected';
            reason: FetchError;
          }>;
          const errorMessages = uniq(rejectedRequests.map((request) => request.reason.responseMessage));

          if (notDeletedNotificationIds.length !== selectedEntities.length) {
            queryClient.invalidateQueries({
              queryKey: EVENT_NOTIFICATIONS_QUERY_KEY,
            });
            refetchEventNotifications();
          }

          UserNotification.error(
            t('bulk.delete_failed', {
              count: selectedItemsAmount,
              failed: notDeletedNotificationIds.length,
              status: errorMessages.join(),
            }),
          );

          return;
        }

        queryClient.invalidateQueries({
          queryKey: EVENT_NOTIFICATIONS_QUERY_KEY,
        });
        setSelectedEntities(notDeletedNotificationIds);
        refetchEventNotifications();
        UserNotification.success(t('bulk.delete_success', { count: selectedItemsAmount }), t('bulk.success_title'));
      });
    }
  }, [
    sendTelemetry,
    t,
    selectedItemsAmount,
    selectedEntities,
    queryClient,
    setSelectedEntities,
    refetchEventNotifications,
  ]);

  return (
    <BulkActionsDropdown>
      <DeleteMenuItem onSelect={() => onDelete()} />
    </BulkActionsDropdown>
  );
};

export default BulkActions;
