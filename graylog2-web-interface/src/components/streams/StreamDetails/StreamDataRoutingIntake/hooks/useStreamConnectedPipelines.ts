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

import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import { defaultOnError } from 'util/conditional/onError';
import type { StreamConnectedPipeline } from 'components/streams/StreamDetails/StreamDataRoutingIntake/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

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
): Promise<PaginatedResponse<StreamConnectedPipeline>> => {
  const response = new Promise((resolve) => {
    resolve({
      query: '',
      pagination: {
        total: 3,
        count: 3,
        page: 1,
        per_page: 20,
      },
      total: 3,
      sort: 'pipeline_rule',
      order: 'asc',
      elements: [
        {
          id: '123',
          pipeline_rule: 'Route to Windows Stream',
          pipeline: 'Winlog Parsing',
          connected_stream: 'Windows Security Logs',
        },
        {
          id: '124',
          pipeline_rule: 'Route to Windows Stream',
          pipeline: 'Winlog Parsing',
          connected_stream: 'Windows Event Logs',
        },
        {
          id: '125',
          pipeline_rule: 'Send to Windows',
          pipeline: 'All messages pipeline',
          connected_stream: 'Default Stream',
        },
      ],
    });
  });

  return response.then(({ elements, query, attributes, pagination: { count, total, page, per_page: perPage } }) => ({
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
};

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
