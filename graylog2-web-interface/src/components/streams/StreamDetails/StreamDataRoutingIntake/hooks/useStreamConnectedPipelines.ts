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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { StreamRoutingRules } from '@graylog/server-api';

import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import { defaultOnError } from 'util/conditional/onError';
import type { StreamConnectedPipeline } from 'components/streams/StreamDetails/StreamDataRoutingIntake/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

type SortType = 'rule' | 'pipeline';

export const STREAM_PIPELINES_QUERY_KEY = 'stream_pipelines';

export const keyFn = (streamId: string, searchParams: SearchParams) => [
  STREAM_PIPELINES_QUERY_KEY,
  streamId,
  searchParams,
];

export type PaginatedStreamConnectedPipelinesResponse = {
  list: Readonly<Array<StreamConnectedPipeline>>;
  pagination: { total: number };
  attributes: Array<Attribute>;
};

export const fetchStreamConnectedPipelines = async (
  streamId: string,
  searchParams: SearchParams,
): Promise<PaginatedResponse<StreamConnectedPipeline>> =>
  StreamRoutingRules.getPage(
    streamId,
    searchParams.sort.attributeId as SortType,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    FiltersForQueryParams(searchParams.filters),
    searchParams.sort.direction,
  ).then(({ elements, query, attributes, pagination: { count, total, page, per_page: perPage } }) => ({
    list: elements,
    attributes: attributes,
    pagination: {
      count,
      total,
      page,
      perPage,
      query,
    },
  }));

const useStreamConnectedPipelines = (
  streamId: string,
  searchParams: SearchParams,
  { enabled } = { enabled: true },
): {
  data: PaginatedStreamConnectedPipelinesResponse;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: keyFn(streamId, searchParams),
    queryFn: () =>
      defaultOnError(
        fetchStreamConnectedPipelines(streamId, searchParams),
        'Loading connected Pipelines failed with status',
        'Could not load connected Pipelines',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return {
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  };
};

export default useStreamConnectedPipelines;
