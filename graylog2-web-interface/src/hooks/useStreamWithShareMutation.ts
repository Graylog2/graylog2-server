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

import UserNotification from 'util/UserNotification';
import type { Stream } from 'logic/streams/types';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { KEY_PREFIX } from 'components/streams/hooks/useStreams';

const createStreamWithShare = async ({ stream, shareRequest }: { stream: Stream; shareRequest: EntitySharePayload }) =>
  Streams.createWithRequest({
    entity: stream,
    share_request: { selected_grantee_capabilities: shareRequest.selected_grantee_capabilities.toJS() },
  });

const useStreamWithShareMutation = () => {
  const queryClient = useQueryClient();
  const addMutation = useMutation(createStreamWithShare, {
    onError: (errorThrown) => {
      UserNotification.error(`Creating stream failed with status: ${errorThrown}`, 'Could not create stream');
    },
    onSuccess: () => {
      queryClient.invalidateQueries(KEY_PREFIX);
      UserNotification.success('Stream has been successfully created.', 'Success!');
    },
  });

  return { createStreamWithShare: addMutation.mutateAsync };
};

export default useStreamWithShareMutation;
