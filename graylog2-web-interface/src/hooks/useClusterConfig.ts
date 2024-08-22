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

import { SystemClusterConfig } from '@graylog/server-api';

const QUERY_KEY = ['system', 'cluster_config'];
const useClusterConfig = <T, >(key: string) => useQuery<T>([...QUERY_KEY, key], () => SystemClusterConfig.read(key) as Promise<T>);

export const useUpdateClusterConfig = <T, >(key: string) => {
  const queryClient = useQueryClient();

  return useMutation((config: T) => SystemClusterConfig.update(key, config), {
    onSuccess: () => {
      queryClient.invalidateQueries([...QUERY_KEY, key]);
    },
  });
};

export default useClusterConfig;
