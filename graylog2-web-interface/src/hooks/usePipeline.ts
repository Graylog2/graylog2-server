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

import { type PipelineType } from 'components/pipelines/types';
import type FetchError from 'logic/errors/FetchError';
import { defaultOnError } from 'util/conditional/onError';

export const PIPELINE_QUERY_KEY = ['pipeline'];

type Options = {
  enabled: boolean;
};

const usePipeline = (
  pipelineId: string,
  { enabled }: Options = { enabled: true },
): {
  data: PipelineType;
  refetch: () => void;
  isInitialLoading: boolean;
} => {
  const { data, refetch, isInitialLoading } = useQuery<PipelineType, FetchError>(
    [...PIPELINE_QUERY_KEY, pipelineId],
    () =>
      defaultOnError(
        PipelinesPipelines.get(pipelineId),
        'Loading pipeline failed with status',
        'Could not load pipeline',
      ),
    {
      enabled,
    },
  );

  return {
    data: data ?? undefined,
    refetch,
    isInitialLoading,
  };
};

export default usePipeline;
