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

import managedConfigurationApi from './managedConfigurationApi';
import type { ManagedConfiguration, ConfigurationValues } from './types';

const getQueryKey = (nodeId: string | null) => ['datanode', 'managed-configuration', nodeId];
const ROLLING_RESTART_QUERY_KEY = ['opensearch-upgrade', 'rolling-restart'];
// While a restart is pending, refetch so the page notices once the Data Nodes picked up their configuration.
const PENDING_RESTART_REFETCH_INTERVAL = 10000;
// While Data Nodes restart, refetch more often so the page unlocks soon after they are back.
const RESTARTING_REFETCH_INTERVAL = 5000;
// While Data Nodes fail to apply settings, refetch so the error disappears once they succeed.
const LIVE_SETTINGS_ERROR_REFETCH_INTERVAL = 10000;
// Data Nodes apply settings which need no restart after saving, so check for errors shortly afterwards.
const LIVE_SETTINGS_CHECK_DELAY = 3000;

type ManagedConfigurationResult = {
  data: ManagedConfiguration | undefined;
  isLoading: boolean;
  isFetching: boolean;
  error: Error | null;
  refetch: () => void;
};

type UpdateManagedConfigurationResult = {
  updateConfiguration: (values: ConfigurationValues) => Promise<ManagedConfiguration>;
};

type RestartDataNodesResult = {
  restartDataNodes: (force?: boolean) => Promise<unknown>;
};

const useManagedConfiguration = (nodeId: string | null = null, enabled = true): ManagedConfigurationResult => {
  const { data, isLoading, isFetching, error, refetch } = useQuery({
    queryKey: getQueryKey(nodeId),
    queryFn: () => managedConfigurationApi.get(nodeId),
    enabled,
    refetchInterval: (query) => {
      if (query.state.data?.restarting_nodes?.length) return RESTARTING_REFETCH_INTERVAL;
      if (query.state.data?.restart_required) return PENDING_RESTART_REFETCH_INTERVAL;

      return Object.keys(query.state.data?.live_settings_errors ?? {}).length
        ? LIVE_SETTINGS_ERROR_REFETCH_INTERVAL
        : false;
    },
  });

  return { data, isLoading, isFetching, error, refetch };
};

export const useUpdateManagedConfiguration = (nodeId: string | null = null): UpdateManagedConfigurationResult => {
  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({
    mutationFn: (values: ConfigurationValues) => managedConfigurationApi.update(values, nodeId),
    onSuccess: async (data) => {
      await queryClient.cancelQueries({ queryKey: getQueryKey(nodeId), exact: true });
      queryClient.setQueryData(getQueryKey(nodeId), data);
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: getQueryKey(nodeId), exact: true });
      }, LIVE_SETTINGS_CHECK_DELAY);
    },
  });

  return { updateConfiguration: mutateAsync };
};

export const useRestartDataNodes = (nodeId: string | null = null): RestartDataNodesResult => {
  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({
    mutationFn: (force: boolean = false) => managedConfigurationApi.restart(nodeId, force),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: getQueryKey(nodeId), exact: true });

      if (nodeId === null) {
        await queryClient.invalidateQueries({ queryKey: ROLLING_RESTART_QUERY_KEY });
      }
    },
  });

  return { restartDataNodes: mutateAsync };
};

export default useManagedConfiguration;
