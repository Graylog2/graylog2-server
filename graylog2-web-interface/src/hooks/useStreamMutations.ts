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
import { useMutation } from '@tanstack/react-query';
import { Streams } from '@graylog/server-api';

import type { Stream as OriginalStream } from 'logic/streams/types';
import UserNotification from 'util/UserNotification';
import { EntityShare } from 'actions/permissions/EntityShareActions';

export type Stream = OriginalStream;

export type StreamConfiguration = Pick<
  Stream,
  | 'index_set_id'
  | 'title'
  | 'matching_type'
  | 'remove_matches_from_default_stream'
  | 'description'
  | 'rules'
  | 'content_pack'
> & EntityShare;

const createStream = async (stream: StreamConfiguration): Promise<{ stream_id: string }> => {
  const { share_request, ...rest } = stream;

  return Streams.create({
    entity: rest,
    share_request: {
      selected_collections: share_request?.selected_collections,
      selected_grantee_capabilities: share_request?.selected_grantee_capabilities.toJS()
    }
  });
};

const useStreamMutations = () => {
  const createMutation = useMutation(createStream, {
    onError: (errorThrown) => {
      UserNotification.error(`Saving Stream failed with status: ${errorThrown}`, 'Could not save Stream');
    },
  });

  return { createStream: createMutation.mutateAsync };
};

export default useStreamMutations;
