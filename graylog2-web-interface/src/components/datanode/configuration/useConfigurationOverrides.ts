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

import configurationOverridesApi from './configurationOverridesApi';
import type { ConfigurationOverrides, ConfigurationOverridesPatch } from './types';

const getQueryKey = (nodeId: string | null) => ['datanode', 'configuration-overrides', nodeId];

type ConfigurationOverridesResult = {
  data: ConfigurationOverrides | undefined;
  isLoading: boolean;
  error: Error | null;
};

type UpdateConfigurationOverridesResult = {
  updateOverrides: (changes: ConfigurationOverridesPatch) => Promise<ConfigurationOverrides>;
};

const useConfigurationOverrides = (nodeId: string | null = null, enabled = true): ConfigurationOverridesResult => {
  const { data, isLoading, error } = useQuery({
    queryKey: getQueryKey(nodeId),
    queryFn: () => configurationOverridesApi.get(nodeId),
    enabled,
  });

  return { data, isLoading, error };
};

export const useUpdateConfigurationOverrides = (nodeId: string | null = null): UpdateConfigurationOverridesResult => {
  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({
    mutationFn: (changes: ConfigurationOverridesPatch) => configurationOverridesApi.update(changes, nodeId),
    onSuccess: (data) => {
      queryClient.setQueryData(getQueryKey(nodeId), data);
    },
  });

  return { updateOverrides: mutateAsync };
};

export default useConfigurationOverrides;
