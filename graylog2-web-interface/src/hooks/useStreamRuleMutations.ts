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

import { StreamRules } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

type StreamRuleData = {
  field: string;
  type: number;
  value: string;
  inverted: boolean;
  description: string;
};

const useStreamRuleMutations = () => {
  const queryClient = useQueryClient();

  const invalidateStreamQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['stream'] });
    queryClient.invalidateQueries({ queryKey: ['streams'] });
  };

  const createRule = useMutation({
    mutationFn: ({ streamId, data }: { streamId: string; data: StreamRuleData }) => StreamRules.create(streamId, data),
    onSuccess: () => invalidateStreamQueries(),
    onError: (error: Error) => {
      UserNotification.error(`Creating Stream Rule failed with status: ${error}`, 'Could not create Stream Rule');
    },
  });

  const updateRule = useMutation({
    mutationFn: ({ streamId, streamRuleId, data }: { streamId: string; streamRuleId: string; data: StreamRuleData }) =>
      StreamRules.update(streamId, streamRuleId, data),
    onSuccess: () => invalidateStreamQueries(),
    onError: (error: Error) => {
      UserNotification.error(`Updating Stream Rule failed with status: ${error}`, 'Could not update Stream Rule');
    },
  });

  const removeRule = useMutation({
    mutationFn: ({ streamId, streamRuleId }: { streamId: string; streamRuleId: string }) =>
      StreamRules.remove(streamId, streamRuleId),
    onSuccess: () => invalidateStreamQueries(),
    onError: (error: Error) => {
      UserNotification.error(`Deleting Stream Rule failed with status: ${error}`, 'Could not delete Stream Rule');
    },
  });

  return {
    createStreamRule: createRule.mutateAsync,
    updateStreamRule: updateRule.mutateAsync,
    removeStreamRule: removeRule.mutateAsync,
  };
};

export default useStreamRuleMutations;
