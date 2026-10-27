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

const useManagedConfiguration = (nodeId: string | null = null): ManagedConfigurationResult => {
  const { data, isLoading, isFetching, error, refetch } = useQuery({
    queryKey: getQueryKey(nodeId),
    queryFn: () => managedConfigurationApi.get(nodeId),
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
    },
  });

  return { updateConfiguration: mutateAsync };
};

export default useManagedConfiguration;
