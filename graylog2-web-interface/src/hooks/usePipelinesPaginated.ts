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

import { PipelinesPipelines } from '@graylog/server-api';

import type { PipelineType } from 'components/pipelines/types';
import type FetchError from 'logic/errors/FetchError';
import { defaultOnError } from 'util/conditional/onError';

import { PIPELINES_QUERY_KEY } from './usePipelines';

type Options = {
  enabled: boolean;
};

type PaginatedPipelinesParams = {
  sort?: 'title' | 'description' | 'id';
  page: number;
  perPage: number;
  query?: string;
  order?: 'asc' | ' desc';
};

const DEFAULT_DATA = {
  total: 0,
  page: 0,
  per_page: 0,
  count: 0,
  pipelines: [],
};

export type PaginatedPipelinesResponse = {
  total: number;
  page: number;
  per_page: number;
  count: number;
  pipelines: Array<PipelineType>;
};

const getPaginatedPipelines = async ({
  sort,
  page,
  perPage,
  query,
  order,
}: PaginatedPipelinesParams): Promise<PaginatedPipelinesResponse> =>
  PipelinesPipelines.getPage(sort, page, perPage, query, order) as Promise<PaginatedPipelinesResponse>;

const usePipelinesPaginated = (
  params: PaginatedPipelinesParams,
  { enabled }: Options = { enabled: true },
): {
  data: PaginatedPipelinesResponse;
  refetch: () => void;
  isInitialLoading: boolean;
} => {
  const { data, refetch, isInitialLoading } = useQuery<PaginatedPipelinesResponse, FetchError>(
    [...PIPELINES_QUERY_KEY, ...Object.values(params)],
    () =>
      defaultOnError(
        getPaginatedPipelines(params),
        'Loading paginated pipelines failed with status',
        'Could not load  paginated pipelines',
      ),
    {
      enabled,
    },
  );

  return {
    data: data ?? DEFAULT_DATA,
    refetch,
    isInitialLoading,
  };
};

export default usePipelinesPaginated;
