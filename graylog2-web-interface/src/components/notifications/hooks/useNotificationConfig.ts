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
    mutationFn: (config) => {
      if (!updateEnabled) {
        return Promise.reject(
          new Error('You do not have permission to update the notifications retention configuration.'),
        );
      }

      return SystemNotifications.updateConfig(config);
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(configKey, updated);
      queryClient.invalidateQueries({ queryKey: configKey });
      UserNotification.success('Notifications configuration updated successfully', 'Success!');
    },
    onError: (error) => {
      if (error?.status === 403) {
        UserNotification.error(
          'You do not have permission to update the notifications retention configuration.',
          'Action not allowed',
        );

        return;
      }

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
