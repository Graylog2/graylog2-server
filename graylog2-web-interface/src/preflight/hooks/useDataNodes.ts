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
import { useState } from 'react';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { DataNodes } from 'preflight/types';
import type FetchError from 'logic/errors/FetchError';
import { onSettled } from 'util/conditional/onError';

const DEFAULT_DATA = [];
export const DATA_NODES_OVERVIEW_QUERY_KEY = ['data-nodes', 'overview'];
const fetchDataNodes = () => (
  fetch('GET', qualifyUrl('/api/data_nodes'), undefined, false)
);

const useDataNodes = (): {
  data: DataNodes,
  isFetching: boolean,
  isInitialLoading: boolean,
  error: FetchError
} => {
  const [metaData, setMetaData] = useState<{
    error: FetchError | null,
    isInitialLoading: boolean,
  }>({
    error: null,
    isInitialLoading: false,
  });
  const {
    data,
    isFetching,
  } = useQuery<DataNodes, FetchError>(
    {
      queryKey: DATA_NODES_OVERVIEW_QUERY_KEY,
      queryFn: () => onSettled(fetchDataNodes(), () => {
        setMetaData({
          error: null,
          isInitialLoading: false,
        });
      }, (newError: FetchError) => {
        setMetaData({
          error: newError,
          isInitialLoading: false,
        });
      }),
      refetchInterval: 3000,
      keepPreviousData: true,
      retry: false,
    });

  return {
    data: data ?? DEFAULT_DATA,
    isFetching,
    isInitialLoading: metaData.isInitialLoading,
    error: metaData.error,
  };
};

export default useDataNodes;
