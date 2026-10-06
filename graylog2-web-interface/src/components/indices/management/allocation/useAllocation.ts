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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import type { AllocationExplanation, RetryFailedResponse, ShardExplanation, ShardMapResponse } from '../types';
import { INDEX_OVERVIEW_QUERY_KEY } from '../useIndexOverview';

const ALLOCATION_URL = '/system/indexer/management/allocation';
const EXPLAIN_QUERY_KEY = ['moremgmt', 'allocation', 'explain'];

// Explaining runs one OpenSearch call per unassigned shard: it runs when the panel opens and on demand, not on
// window focus or a timer.
export const useAllocationExplain = (
  enabled: boolean,
): {
  explanation: AllocationExplanation | undefined;
  error: Error | null;
  isFetching: boolean;
  refetch: () => void;
} => {
  const { data, error, isFetching, refetch } = useQuery({
    queryKey: EXPLAIN_QUERY_KEY,
    queryFn: () => fetch<AllocationExplanation>('GET', qualifyUrl(`${ALLOCATION_URL}/explain`)),
    enabled,
    refetchOnWindowFocus: false,
  });

  return { explanation: data, error, isFetching, refetch };
};

const MAP_QUERY_KEY = ['moremgmt', 'allocation', 'map'];
const MAP_REFETCH_INTERVAL_MS = 30000;

// Two _cat calls however big the cluster: cheap enough to refresh like the index list.
export const useShardMap = (
  enabled: boolean,
): {
  map: ShardMapResponse | undefined;
  error: Error | null;
  isFetching: boolean;
  refetch: () => void;
} => {
  const { data, error, isFetching, refetch } = useQuery({
    queryKey: MAP_QUERY_KEY,
    queryFn: () => fetch<ShardMapResponse>('GET', qualifyUrl(`${ALLOCATION_URL}/map`)),
    enabled,
    refetchInterval: MAP_REFETCH_INTERVAL_MS,
  });

  return { map: data, error, isFetching, refetch };
};

// One explain call, only for the copy the user opened on the map.
export const useShardExplanation = (
  copy: { index: string; shard: number; primary: boolean } | undefined,
): { explanation: ShardExplanation | undefined; error: Error | null; isLoading: boolean } => {
  const { data, error, isLoading } = useQuery({
    queryKey: [...EXPLAIN_QUERY_KEY, copy?.index, copy?.shard, copy?.primary],
    queryFn: () =>
      fetch<ShardExplanation>(
        'GET',
        qualifyUrl(
          `${ALLOCATION_URL}/explain/${encodeURIComponent(copy.index)}/${copy.shard}?primary=${copy.primary}`,
        ),
      ),
    enabled: copy !== undefined,
    refetchOnWindowFocus: false,
  });

  return { explanation: data, error, isLoading };
};

export const useRetryFailedAllocations = (): {
  retryFailed: () => Promise<RetryFailedResponse>;
  isRetrying: boolean;
} => {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending } = useMutation({
    mutationFn: () => fetch<RetryFailedResponse>('POST', qualifyUrl(`${ALLOCATION_URL}/retry_failed`)),
    onSuccess: (response) => {
      if (response.acknowledged) {
        UserNotification.success(
          'OpenSearch is retrying failed shard allocations. Explain again in a moment to see the outcome.',
          'Retry requested',
        );
      } else {
        UserNotification.warning('OpenSearch did not acknowledge the retry request.', 'Retry not acknowledged');
      }
    },
    onError: (error) => UserNotification.error(`Retrying failed allocations failed: ${error.message}`),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: INDEX_OVERVIEW_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: EXPLAIN_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: MAP_QUERY_KEY });
    },
  });

  return { retryFailed: () => mutateAsync(), isRetrying: isPending };
};
