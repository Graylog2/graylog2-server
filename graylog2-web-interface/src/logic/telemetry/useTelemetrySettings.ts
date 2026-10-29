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

import { Telemetry } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { StubResponse } from 'util/UtilityTypes';

export type UserTelemetrySettings = StubResponse<typeof Telemetry.getTelemetryUserSettings>;

const TELEMETRY_SETTINGS_QUERY_KEY = ['telemetry', 'settings'] as const;

export const fetchTelemetrySettings = (): Promise<UserTelemetrySettings> => Telemetry.getTelemetryUserSettings();

export const updateTelemetrySettings = (settings: UserTelemetrySettings): Promise<void> =>
  Telemetry.saveTelemetryUserSettings(settings);

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
