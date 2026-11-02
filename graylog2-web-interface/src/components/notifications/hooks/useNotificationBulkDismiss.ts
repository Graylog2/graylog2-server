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

import i18n from 'i18n';
import UserNotification from 'util/UserNotification';
import type FetchError from 'logic/errors/FetchError';
import { NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY, TABLE_KEY } from 'components/notifications/constants';

const useNotificationBulkDismiss = () => {
  const queryClient = useQueryClient();
  const tableKey = [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] as const;
  const badgeKey = [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY] as const;

  return useMutation<unknown, FetchError, { entity_ids: string[] }>({
    mutationFn: ({ entity_ids }) => SystemNotifications.bulkDelete({ entity_ids }),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tableKey });
      queryClient.invalidateQueries({ queryKey: badgeKey });
    },

    onError: (error) => {
      if (error?.status === 403) return;

      if (error?.status === 400) {
        UserNotification.warning(
          i18n.t('notifications.nothing_selected', { ns: 'system' }),
          i18n.t('notifications.nothing_to_dismiss_title', { ns: 'system' }),
        );

        return;
      }

      UserNotification.error(
        i18n.t('notifications.bulk_dismiss_failed', { ns: 'system' }),
        i18n.t('notifications.dismiss_failed_title', { ns: 'system' }),
      );
    },
  });
};

export default useNotificationBulkDismiss;
