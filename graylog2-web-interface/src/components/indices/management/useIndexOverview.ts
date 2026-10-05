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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

import type { IndexOverviewResponse, IndexSummary } from './types';

export const INDEX_OVERVIEW_URL = '/system/indexer/management/indices';
export const INDEX_OVERVIEW_QUERY_KEY = ['moremgmt', 'indices'];
const REFETCH_INTERVAL_MS = 30000;

const useIndexOverview = (): {
  indices: Array<IndexSummary>;
  error: Error | null;
  isLoading: boolean;
  isFetching: boolean;
  refetch: () => void;
} => {
  const { data, error, isLoading, isFetching, refetch } = useQuery({
    queryKey: INDEX_OVERVIEW_QUERY_KEY,
    queryFn: () => fetch<IndexOverviewResponse>('GET', qualifyUrl(INDEX_OVERVIEW_URL)),
    refetchInterval: REFETCH_INTERVAL_MS,
  });

  return { indices: data?.indices ?? [], error, isLoading, isFetching, refetch };
};

export default useIndexOverview;
