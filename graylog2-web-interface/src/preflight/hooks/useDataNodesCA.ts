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

import { useQuery } from '@tanstack/react-query';

import type { DataNodesCAStatus } from 'preflight/types';
import type FetchError from 'logic/errors/FetchError';

const fetchDataNodesCA = async (): Promise<DataNodesCAStatus> => (
  // fetch('GET', qualifyUrl('/api/preflight/ca'))
  Promise.resolve({ isConfigured: false })
);

const useDataNodesCA = (): {
  data: DataNodesCAStatus,
  isFetching: boolean,
  error: FetchError,
  isInitialLoading: boolean
} => {
  const {
    data,
    isFetching,
    error,
    isInitialLoading,
  } = useQuery<DataNodesCAStatus, FetchError>({
    queryKey: ['data-nodes', 'ca-status'],
    queryFn: fetchDataNodesCA,
    initialData: { isConfigured: false },
    retry: 3000,
  });

  return { data, isFetching, error, isInitialLoading };
};

export default useDataNodesCA;
