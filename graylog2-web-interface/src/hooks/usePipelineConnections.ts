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
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { PipelinesConnections } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import { defaultOnError } from 'util/conditional/onError';

export type PipelineConnectionsType = {
  id?: string;
  stream_id: string;
  pipeline_ids: string[];
};

export const PIPELINE_CONNECTIONS_QUERY_KEY = ['pipeline', 'connections'];

const usePipelineConnections = () => {
  const { data, isLoading } = useQuery({
    queryKey: PIPELINE_CONNECTIONS_QUERY_KEY,
    queryFn: () =>
      defaultOnError(
        PipelinesConnections.getAll() as Promise<PipelineConnectionsType[]>,
        'Fetching pipeline connections failed with status',
        'Could not retrieve pipeline connections',
      ),
  });

  return { data: data ?? [], isLoading };
};

export const usePipelineConnectionMutation = () => {
  const queryClient = useQueryClient();

  const connectToPipeline = useMutation({
    mutationFn: (reverseConnection: { pipeline: string; streams: string[] }) =>
      PipelinesConnections.connectStreams({
        pipeline_id: reverseConnection.pipeline,
        stream_ids: reverseConnection.streams,
      }),
    onSuccess: () => {
      UserNotification.success('Pipeline connections updated successfully');
      queryClient.invalidateQueries({ queryKey: PIPELINE_CONNECTIONS_QUERY_KEY });
    },
    onError: (error: Error) => {
      UserNotification.error(
        `Updating pipeline connections failed with status: ${error.message}`,
        'Could not update pipeline connections',
      );
    },
  });

  return {
    connectToPipeline: connectToPipeline.mutateAsync,
  };
};

export default usePipelineConnections;
