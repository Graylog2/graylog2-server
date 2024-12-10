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
import { create, windowScheduler, indexedResolver } from '@yornaath/batshit';

import { Streams } from '@graylog/server-api';

import type FetchError from 'logic/errors/FetchError';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';

export type StreamConnectedPipelines = Array<Pick<PipelineType, 'id' | 'title'>>

const pipelines = create({
  fetcher: async (streamIds: Array<string>) => Streams.getConnectedPipelinesForStreams({ stream_ids: streamIds }),
  resolver: indexedResolver(),
  scheduler: windowScheduler(10),
});

const usePipelinesConnectedStream = (streamId: string, enabled: boolean = true): {
  data: StreamConnectedPipelines,
  refetch: () => void,
  isInitialLoading: boolean,
  error: FetchError,
  isError: boolean,
} => {
  const { data, refetch, isInitialLoading, error, isError } = useQuery<StreamConnectedPipelines, FetchError>(
    ['stream', 'pipelines', streamId],
    () => pipelines.fetch(streamId),
    {
      notifyOnChangeProps: ['data', 'error'],
      enabled: enabled,
    },
  );

  return ({
    data: data ?? [],
    refetch,
    isInitialLoading,
    error,
    isError,
  });
};

export default usePipelinesConnectedStream;
