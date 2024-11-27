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

import type { DataNodesCA } from 'preflight/types';
import type FetchError from 'logic/errors/FetchError';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { onSettled } from 'util/conditional/onError';

export const QUERY_KEY = ['data-nodes', 'ca-status'];
const fetchDataNodesCA = (): Promise<DataNodesCA> => (
  fetch('GET', qualifyUrl('/api/ca'), undefined, false)
);

const useDataNodesCA = (): {
  data: DataNodesCA,
  isFetching: boolean,
  error: FetchError,
  isInitialLoading: boolean
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
  } = useQuery<DataNodesCA, FetchError>({
    queryKey: QUERY_KEY,
    queryFn: () => onSettled(fetchDataNodesCA(), () => {
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
    initialData: undefined,
    refetchInterval: 3000,
    retry: false,
  });

  return {
    data,
    isFetching,
    isInitialLoading: metaData.isInitialLoading,
    error: metaData.error,
  };
};

export default useDataNodesCA;
