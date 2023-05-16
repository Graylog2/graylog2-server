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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { DataNodes } from 'preflight/types';
import type FetchError from 'logic/errors/FetchError';

const DEFAULT_DATA = [];
const fetchDataNodes = () => (
  fetch('GET', qualifyUrl('/api/data_nodes'), undefined, false)
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
  } = useQuery<DataNodes, FetchError>(
    ['data-nodes', 'overview'],
    fetchDataNodes,
    {
      refetchInterval: 3000,
      keepPreviousData: true,
    });

  return { data: data ?? DEFAULT_DATA, isFetching, isInitialLoading, error };
};

export default useDataNodes;
