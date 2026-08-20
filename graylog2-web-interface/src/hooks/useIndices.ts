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
import ApiRoutes from 'routing/ApiRoutes';
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';

export const TIME_BASED_ROTATION_STRATEGY = 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy';
export const NOOP_RETENTION_STRATEGY = 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy';
export const TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY =
  'org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy';
export const ARCHIVE_RETENTION_STRATEGY =
  'org.graylog.plugins.archive.indexer.retention.strategies.ArchiveRetentionStrategy';
export const TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY_TYPE =
  'org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig';
export const RETENTION = 'retention';

export const INDICES_QUERY_KEY = ['indices'] as const;

export type IndexTimeAndTotalStats = {
  total: number;
  time_seconds: number;
};

export type IndexShardRouting = {
  id: number;
  state: string;
  active: boolean;
  primary: boolean;
  node_id: string;
  node_name: string;
  node_hostname: string;
  relocating_to: string | null;
};

export type IndexInfo = {
  index_name: string;
  primary_shards: {
    flush: IndexTimeAndTotalStats;
    get: IndexTimeAndTotalStats;
    index: IndexTimeAndTotalStats;
    merge: IndexTimeAndTotalStats;
    refresh: IndexTimeAndTotalStats;
    search_query: IndexTimeAndTotalStats;
    search_fetch: IndexTimeAndTotalStats;
    open_search_contexts: number;
    store_size_bytes: number;
    segments: number;
    documents: {
      count: number;
      deleted: number;
    };
  };
  all_shards: {
    flush: IndexTimeAndTotalStats;
    get: IndexTimeAndTotalStats;
    index: IndexTimeAndTotalStats;
    merge: IndexTimeAndTotalStats;
    refresh: IndexTimeAndTotalStats;
    search_query: IndexTimeAndTotalStats;
    search_fetch: IndexTimeAndTotalStats;
    open_search_contexts: number;
    store_size_bytes: number;
    segments: number;
    documents: {
      count: number;
      deleted: number;
    };
  };
  routing: Array<IndexShardRouting>;
  is_reopened: boolean;
};

export type Indices = Array<IndexInfo>;

type IndicesListResponse = {
  all: {
    indices: Indices;
  };
  closed: {
    indices: Indices;
  };
};

export const fetchIndices = (indexSetId: string): Promise<IndicesListResponse> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndicesApiController.list(indexSetId).url));

export const fetchAllIndices = (): Promise<IndicesListResponse> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndicesApiController.listAll().url));

export const fetchMultipleIndices = (indexNames: string[]): Promise<Indices> =>
  fetchPeriodically('POST', qualifyUrl(ApiRoutes.IndicesApiController.multiple().url), { indices: indexNames });

export const closeIndex = (indexName: string): Promise<unknown> =>
  fetch('POST', qualifyUrl(ApiRoutes.IndicesApiController.close(indexName).url));

export const deleteIndex = (indexName: string): Promise<unknown> =>
  fetch('DELETE', qualifyUrl(ApiRoutes.IndicesApiController.delete(indexName).url));

export const reopenIndex = (indexName: string): Promise<unknown> =>
  fetch('POST', qualifyUrl(ApiRoutes.IndicesApiController.reopen(indexName).url));

type ListResult = {
  indices: Indices | undefined;
  closedIndices: Indices | undefined;
};

export const useIndices = (indexSetId: string) =>
  useQuery({
    queryKey: [...INDICES_QUERY_KEY, 'list', indexSetId],
    queryFn: () => fetchIndices(indexSetId),
    select: (response): ListResult => ({
      indices: response.all.indices,
      closedIndices: response.closed.indices,
    }),
    enabled: !!indexSetId,
  });

export const useAllIndices = () =>
  useQuery({
    queryKey: [...INDICES_QUERY_KEY, 'listAll'],
    queryFn: fetchAllIndices,
    select: (response): ListResult => ({
      indices: response.all.indices,
      closedIndices: response.closed.indices,
    }),
  });

export const useMultipleIndices = (indexNames: string[]) =>
  useQuery({
    queryKey: [...INDICES_QUERY_KEY, 'multiple', [...indexNames].sort()],
    queryFn: () => fetchMultipleIndices(indexNames),
    enabled: indexNames.length > 0,
    refetchInterval: 5000,
  });
