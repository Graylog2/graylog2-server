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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { SystemNotifications } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type FetchError from 'logic/errors/FetchError';
import type { NotificationType } from 'components/notifications/types';
import { NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY, TABLE_KEY } from 'components/notifications/constants';

const useNotificationToggleRead = () => {
  const queryClient = useQueryClient();
  const tableKey = [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] as const;
  const badgeKey = [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY] as const;

  return useMutation<NotificationType, FetchError, { id: string; currentIsRead: boolean }>({
    mutationFn: ({ id }) => SystemNotifications.toggleRead(id) as Promise<NotificationType>,

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tableKey });
      queryClient.invalidateQueries({ queryKey: badgeKey });
    },

    onError: (error) => {
      if (error?.status === 403) return;

      if (error?.status === 404) {
        UserNotification.warning('Notification no longer exists.', 'Notification not found');
        queryClient.invalidateQueries({ queryKey: tableKey });

        return;
      }

      UserNotification.error('Failed to update notification read state. Please try again.', 'Update failed');
    },
  });
};

export default useNotificationToggleRead;
