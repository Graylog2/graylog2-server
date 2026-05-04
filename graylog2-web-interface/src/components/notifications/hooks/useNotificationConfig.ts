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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { SystemNotifications } from '@graylog/server-api';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type FetchError from 'logic/errors/FetchError';
import type { SystemNotificationConfig } from 'components/notifications/types';
import {
  NOTIFICATIONS_QUERY_KEY,
  CONFIG_KEY,
} from 'components/notifications/constants';

type Options = {
  readEnabled?: boolean;
  updateEnabled?: boolean;
};

const useNotificationConfig = ({
  readEnabled = true,
  updateEnabled = true,
}: Options = {}) => {
  const queryClient = useQueryClient();
  const configKey = [...NOTIFICATIONS_QUERY_KEY, CONFIG_KEY] as const;

  const query = useQuery<SystemNotificationConfig, FetchError>({
    queryKey: configKey,
    queryFn: () => SystemNotifications.getConfig(),
    enabled: readEnabled,
  });

  const mutation = useMutation<SystemNotificationConfig, FetchError, SystemNotificationConfig>({
    // PUT body is `{ retention_days }`. The generated SDK's `updateConfig` does
    // not accept the body argument, so we drop down to fetch directly. The
    // response shape is still typed via the SDK derivation in
    // `components/notifications/types.ts`.
    mutationFn: (config) => fetch('PUT', '/system/notifications/config', config),
    onSuccess: (updated) => {
      queryClient.setQueryData(configKey, updated);
      queryClient.invalidateQueries({ queryKey: configKey });
    },
    onError: (error) => {
      if (error?.status === 403) {
        UserNotification.error(
          'You do not have permission to update the notifications retention configuration.',
          'Action not allowed',
        );

        return;
      }

      // 400 surfaces server-side validation messages; let consumers display
      // them inline rather than as a generic toast.
      if (error?.status !== 400) {
        UserNotification.error(
          'Failed to update notifications retention configuration. Please try again.',
          'Update failed',
        );
      }
    },
  });

  return {
    config: query.data,
    isLoading: query.isLoading,
    error: query.error,
    update: mutation.mutateAsync,
    isUpdating: mutation.isPending,
    updateError: mutation.error,
    isUpdateEnabled: updateEnabled,
  };
};

export default useNotificationConfig;
