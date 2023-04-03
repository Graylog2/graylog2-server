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

import type { DataNodes } from 'preflight/types';
import type FetchError from 'logic/errors/FetchError';

const availableDataNodes = [
  { id: 'data-node-id-1', transportAddress: 'transport.address1', isSecured: false },
  { id: 'data-node-id-2', transportAddress: 'transport.address2', isSecured: false },
  { id: 'data-node-id-3', transportAddress: 'transport.address3', isSecured: false },
];

const fetchDataNodes = async (): Promise<DataNodes> => (
  // fetch('GET', qualifyUrl('/api/preflight/datanodes'))
  Promise.resolve(availableDataNodes)
);

const useDataNodes = (): {
  data: DataNodes,
  isFetching: boolean,
  isInitialLoading: boolean,
  error: FetchError
} => {
  const {
    data,
    isFetching,
    error,
    isInitialLoading,
  } = useQuery<DataNodes, FetchError>({
    queryKey: ['data-nodes', 'overview'],
    queryFn: fetchDataNodes,
    initialData: [],
    retry: 3000,
  });

  return { data, isFetching, isInitialLoading, error };
};

export default useDataNodes;
