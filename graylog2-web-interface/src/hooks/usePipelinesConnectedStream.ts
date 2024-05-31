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
import { qualifyUrl } from 'util/URLUtils';

import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type FetchError from 'logic/errors/FetchError';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';

export type StreamConnectedPipelines = Array<Pick<PipelineType, 'id' | 'title'>>

const fetchPipelinesConnectedStream = (streamId: string) => fetch('GET', qualifyUrl(ApiRoutes.StreamsApiController.stream_connected_pipelines(streamId).url));

const usePipelinesConnectedStream = (streamId: string): {
  data: StreamConnectedPipelines,
  refetch: () => void,
  isInitialLoading: boolean,
  error: FetchError,
} => {
  const { data, refetch, isInitialLoading, error } = useQuery<StreamConnectedPipelines, FetchError>(
    ['stream', 'pipelines', streamId],
    () => fetchPipelinesConnectedStream(streamId),
    {
      notifyOnChangeProps: ['data', 'error'],
    },
  );

  return ({
    data,
    refetch,
    isInitialLoading,
    error,
  });
};

export default usePipelinesConnectedStream;
