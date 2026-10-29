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

import { SystemRuntimeTunables } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

import { RUNTIME_TUNABLES_KEY } from './useRuntimeTunables';
import type { TunableValues } from './types';

const useRuntimeTunableMutations = (): {
  updateTunable: (args: { id: string; values: TunableValues }) => Promise<unknown>;
  resetTunable: (id: string) => Promise<unknown>;
} => {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: RUNTIME_TUNABLES_KEY });

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: string; values: TunableValues }) => SystemRuntimeTunables.update(id, values),
    onSuccess: () => {
      invalidate();
      UserNotification.success('Configuration updated successfully');
    },
  });
  const resetMutation = useMutation({
    mutationFn: (id: string) => SystemRuntimeTunables.reset(id),
    onSuccess: () => {
      invalidate();
      UserNotification.success('Configuration reset to defaults');
    },
    onError: (error: Error) =>
      UserNotification.error(`Resetting configuration failed: ${error}`, 'Could not reset configuration'),
  });

  return { updateTunable: updateMutation.mutateAsync, resetTunable: resetMutation.mutateAsync };
};

export default useRuntimeTunableMutations;
