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

import { SystemOutputs } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import { KEY_PREFIX } from 'hooks/useOutputs';

const useOutputMutations = () => {
  const queryClient = useQueryClient();

  const invalidateOutputQueries = () => {
    queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
  };

  const saveMutation = useMutation({
    mutationFn: (output: { title: string; type: string; configuration: Record<string, unknown> }) =>
      SystemOutputs.create(output as any),
    onSuccess: () => invalidateOutputQueries(),
    onError: (error: Error, output) => {
      UserNotification.error(`Saving Output "${output.title}" failed with status: ${error}`, 'Could not save Output');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ outputId, deltas }: { outputId: string; title: string; deltas: any }) =>
      SystemOutputs.update(outputId, deltas),
    onSuccess: () => invalidateOutputQueries(),
    onError: (error: Error, { title }) => {
      UserNotification.error(`Updating Output "${title}" failed with status: ${error}`, 'Could not update Output');
    },
  });

  const removeMutation = useMutation({
    mutationFn: (outputId: string) => SystemOutputs.remove(outputId),
    onSuccess: () => invalidateOutputQueries(),
    onError: (error: Error) => {
      UserNotification.error(`Terminating output failed with status: ${error}`, 'Could not terminate output');
    },
  });

  return {
    saveOutput: saveMutation.mutateAsync,
    updateOutput: updateMutation.mutateAsync,
    removeOutput: removeMutation.mutateAsync,
  };
};

export default useOutputMutations;
