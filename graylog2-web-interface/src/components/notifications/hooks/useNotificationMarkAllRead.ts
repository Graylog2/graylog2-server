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
import {
  NOTIFICATIONS_QUERY_KEY,
  BADGE_COUNT_KEY,
  TABLE_KEY,
} from 'components/notifications/constants';

const useNotificationMarkAllRead = () => {
  const queryClient = useQueryClient();
  const tableKey = [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] as const;
  const badgeKey = [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY] as const;

  // One-directional: backend marks every notification the user can see as read.
  // The consumer (table toolbar) MUST gate this behind a confirmation modal —
  // see Phase 2 MarkAllAsReadConfirmationModal — so we do NOT optimistic-patch
  // here. After the 204 response we just invalidate; the table re-fetches and
  // shows the post-mark-all state.
  return useMutation<unknown, FetchError, void>({
    mutationFn: () => SystemNotifications.readAll(),

    onError: (error) => {
      if (error?.status === 403) {
        UserNotification.error(
          'You do not have permission to mark notifications as read.',
          'Action not allowed',
        );

        return;
      }

      UserNotification.error(
        'Failed to mark all notifications as read. Please try again.',
        'Mark all failed',
      );
    },

    onSettled: (_data, error) => {
      // Skip invalidation on 403 — no server state changed.
      if (error?.status === 403) return;

      queryClient.invalidateQueries({ queryKey: tableKey });
      queryClient.invalidateQueries({ queryKey: badgeKey });
    },
  });
};

export default useNotificationMarkAllRead;
