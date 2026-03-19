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
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

export type UserTelemetrySettings = {
  telemetry_permission_asked: boolean;
  telemetry_enabled: boolean;
};

export type TelemetrySettingsStoreState = {
  telemetrySetting: UserTelemetrySettings;
};

const TELEMETRY_SETTINGS_QUERY_KEY = ['telemetry', 'settings'] as const;

const settingsUrl = () => qualifyUrl(ApiRoutes.TelemetryApiController.setting().url);

export const fetchTelemetrySettings = (): Promise<UserTelemetrySettings> => fetch('GET', settingsUrl());

export const updateTelemetrySettings = (settings: Partial<UserTelemetrySettings>): Promise<UserTelemetrySettings> =>
  fetch('PUT', settingsUrl(), settings);

const useTelemetrySettings = (options?: { enabled?: boolean }) =>
  useQuery({
    queryKey: TELEMETRY_SETTINGS_QUERY_KEY,
    queryFn: fetchTelemetrySettings,
    ...options,
  });

export const useUpdateTelemetrySettings = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateTelemetrySettings,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TELEMETRY_SETTINGS_QUERY_KEY }),
    onError: (error) => {
      UserNotification.error(`Update failed: ${error}`, 'Could not update telemetry settings.');
    },
  });
};

export default useTelemetrySettings;
