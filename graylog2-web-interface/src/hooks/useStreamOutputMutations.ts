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

import { StreamOutputs } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

const addStreamOutput = async ({ streamId, outputs }: { streamId: string, outputs: { outputs: Array<string>}}) => StreamOutputs.add(streamId, outputs);
const removeStreamOutput = async ({ streamId, outputId }: { streamId: string, outputId: string}) => StreamOutputs.remove(streamId, outputId);

const useStreamOutputMutation = () => {
  const addMutation = useMutation(addStreamOutput, {
    onError: (errorThrown) => {
      UserNotification.error(`Adding output to stream failed with status: ${errorThrown}`,
        'Could not add output to stream');
    },
    onSuccess: () => {
      UserNotification.success('Output has been successfully added to Stream.', 'Success!');
    },

  });

  const removeMutation = useMutation(removeStreamOutput, {
    onError: (errorThrown) => {
      UserNotification.error(`Deleting output from stream failed with status: ${errorThrown}`,
        'Could not delete output from stream');
    },
    onSuccess: () => {
      UserNotification.success('Output has been successfully removed from stream.', 'Success!');
    },
  });

  return { addStreamOutput: addMutation.mutateAsync, removeStreamOutput: removeMutation.mutateAsync };
};

export default useStreamOutputMutation;
