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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { Streams } from '@graylog/server-api';

import type { Stream as OriginalStream, StreamConfiguration } from 'logic/streams/types';
import UserNotification from 'util/UserNotification';
import type { EntityShare } from 'actions/permissions/EntityShareActions';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import {
  updateStream as updateStreamApi,
  removeStream as removeStreamApi,
  pauseStream as pauseStreamApi,
  resumeStream as resumeStreamApi,
  cloneStream as cloneStreamApi,
} from 'api/streams';
import type { CloneStreamRequest } from 'api/streams';

export type Stream = OriginalStream;

const createStream = async (stream: StreamConfiguration & EntityShare): Promise<{ stream_id: string }> => {
  const { share_request, ...rest } = stream;

  return Streams.create({
    entity: rest,
    share_request: {
      selected_collections: share_request?.selected_collections,
      selected_grantee_capabilities: share_request?.selected_grantee_capabilities?.toJS(),
    },
  });
};

const useStreamMutations = () => {
  const queryClient = useQueryClient();

  const invalidateStreamQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['streams'] });
  };

  const invalidateSingleStreamQueries = (streamId: string) => {
    invalidateStreamQueries();
    queryClient.invalidateQueries({ queryKey: ['stream', streamId] });
  };

  const createMutation = useMutation({
    mutationFn: createStream,
    onSuccess: () => {
      invalidateStreamQueries();
      CurrentUserStore.reload();
    },
    onError: (errorThrown) => {
      UserNotification.error(`Saving Stream failed with status: ${errorThrown}`, 'Could not save Stream');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ streamId, data }: { streamId: string; data: Partial<Stream> }) => updateStreamApi(streamId, data),
    onSuccess: (_data, { streamId }) => invalidateSingleStreamQueries(streamId),
  });

  const removeMutation = useMutation({
    mutationFn: (streamId: string) => removeStreamApi(streamId),
    onSuccess: (_data, streamId) => invalidateSingleStreamQueries(streamId),
  });

  const pauseMutation = useMutation({
    mutationFn: (streamId: string) => pauseStreamApi(streamId),
    onSuccess: (_data, streamId) => invalidateSingleStreamQueries(streamId),
  });

  const resumeMutation = useMutation({
    mutationFn: (streamId: string) => resumeStreamApi(streamId),
    onSuccess: (_data, streamId) => invalidateSingleStreamQueries(streamId),
  });

  const cloneMutation = useMutation({
    mutationFn: ({ streamId, data }: { streamId: string; data: CloneStreamRequest }) => cloneStreamApi(streamId, data),
    onSuccess: () => invalidateStreamQueries(),
  });

  return {
    createStream: createMutation.mutateAsync,
    updateStream: updateMutation.mutateAsync,
    removeStream: removeMutation.mutateAsync,
    pauseStream: pauseMutation.mutateAsync,
    resumeStream: resumeMutation.mutateAsync,
    cloneStream: cloneMutation.mutateAsync,
  };
};

export default useStreamMutations;
